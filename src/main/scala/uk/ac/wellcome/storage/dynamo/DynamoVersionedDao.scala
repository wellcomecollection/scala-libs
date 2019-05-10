package uk.ac.wellcome.storage.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{
  ConditionalCheckFailedException,
  ProvisionedThroughputExceededException
}
import com.gu.scanamo.error.{
  ConditionNotMet,
  ScanamoError
}
import com.gu.scanamo.ops.ScanamoOps
import com.gu.scanamo.query.{KeyEquals, UniqueKey}
import com.gu.scanamo.syntax.{attributeExists, not, _}
import com.gu.scanamo.{DynamoFormat, Scanamo, Table}
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.VersionedDao
import uk.ac.wellcome.storage.type_classes.{
  IdGetter,
  VersionGetter,
  VersionUpdater
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DynamoVersionedDao[T](
  dynamoDbClient: AmazonDynamoDB,
  dynamoConfig: DynamoConfig
)(implicit
  ec: ExecutionContext,
  evidence: DynamoFormat[T],
  versionUpdater: VersionUpdater[T],
  idGetter: IdGetter[T],
  versionGetter: VersionGetter[T],
  updateExpressionGenerator: UpdateExpressionGenerator[T])
    extends Logging with VersionedDao[T] {

  val table: Table[T] = Table[T](dynamoConfig.table)

  def put(value: T): Try[T] = Try {
    val id = idGetter.id(value)
    debug(s"Attempting to update Dynamo record: $id")

    val ops = updateBuilder(value)
    Scanamo.exec(dynamoDbClient)(ops) match {
      case Left(ConditionNotMet(exc: ConditionalCheckFailedException)) =>
        warn("Failed a conditional check", exc)
        throw exc
      case Left(scanamoError) =>
        val exception = new RuntimeException(scanamoError.toString)

        warn(s"Failed to update Dynamo record: $id", exception)

        throw exception
      case Right(result) =>
        debug(s"Successfully updated Dynamo record: $id")
        result
    }
  }

  def get(id: String): Try[Option[T]] = Try {
    debug(s"Attempting to retrieve Dynamo record: $id")
    Scanamo.exec(dynamoDbClient)(table.get('id -> id)) match {
      case Some(Right(record)) => {
        debug(s"Successfully retrieved Dynamo record: $id")

        Some(record)
      }
      case Some(Left(scanamoError)) =>
        val exception = new RuntimeException(scanamoError.toString)

        error(
          s"An error occurred while retrieving $id from DynamoDB",
          exception
        )

        throw exception
      case None => {
        debug(s"No Dynamo record found for id: $id")

        None
      }
    }
  }

  @deprecated("Use put() instead of updateRecord()", since = "2019-05-10")
  def updateRecord[R](record: T): Future[T] =
    Future
      .fromTry { put(record) }
      .recover {
        case t: ProvisionedThroughputExceededException =>
          throw DynamoNonFatalError(t)
      }

  @deprecated("Use get() instead of getRecord()", since = "2019-05-10")
  def getRecord[R](id: String): Future[Option[T]] =
    Future
      .fromTry { get(id) }
      .recover {
        case t: ProvisionedThroughputExceededException =>
          throw DynamoNonFatalError(t)
      }

  private def updateBuilder(record: T): ScanamoOps[Either[ScanamoError, T]] = {
    val version = versionGetter.version(record)
    val newVersion = version + 1

    val updatedRecord = versionUpdater.updateVersion(record, newVersion)

    updateExpressionGenerator
      .generateUpdateExpression(updatedRecord)
      .map { updateExpression =>
        Table[T](dynamoConfig.table)
          .given(
            not(attributeExists('id)) or
              (attributeExists('id) and 'version < newVersion)
          )
          .update(
            UniqueKey(KeyEquals('id, idGetter.id(record))),
            updateExpression
          )
      }
      .getOrElse(
        // Everything that gets passed into updateBuilder should have an "id"
        // and a "version" field, and the compiler enforces this with the
        // implicit IdGetter[T] and VersionGetter[T].
        //
        // generateUpdateExpression returns None only if the record only
        // contains an "id" field, so we should always get Some(ops) out
        // of this function.
        //
        throw new Exception(
          "Trying to update a record that only has an id: this should be impossible!")
      )
  }
}
