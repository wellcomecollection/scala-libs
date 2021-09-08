package weco.storage.store.dynamo

import org.scanamo.query.UniqueKey
import org.scanamo.syntax._
import org.scanamo.{DynamoFormat, Scanamo, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import weco.storage.dynamo.{DynamoHashEntry, DynamoHashRangeEntry}
import weco.storage.store.Readable
import weco.storage._

import scala.util.{Failure, Success, Try}

sealed trait ConsistencyMode
case object StronglyConsistent extends ConsistencyMode
case object EventuallyConsistent extends ConsistencyMode

sealed trait DynamoReadable[Ident, DynamoIdent, EntryType, T]
    extends Readable[Ident, T] {

  implicit protected val format: DynamoFormat[EntryType]

  // DynamoDB supports eventually consistent and strongly consistent reads.
  //
  //  * Eventually consistent might not return data from a recent write operation
  //  * Strongly consistent returns the most recent data, but with higher latency
  //    and using more throughput capacity.
  //
  // Our default is the same as DynamoDB (eventually consistent), but we allow
  // overriding it if necessary.
  //
  // See https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.ReadConsistency.html
  protected val consistencyMode: ConsistencyMode = EventuallyConsistent

  protected val client: DynamoDbClient
  protected val table: Table[EntryType]

  protected def createKeyExpression(id: DynamoIdent): UniqueKey[_]

  protected def getEntry(id: DynamoIdent): Either[ReadError, EntryType] = {
    val ops = consistencyMode match {
      case EventuallyConsistent => table.get(createKeyExpression(id))
      case StronglyConsistent =>
        table.consistently.get(createKeyExpression(id))
    }

    Try(Scanamo(client).exec(ops)) match {
      case Success(Some(Right(entry))) => Right(entry)

      case Success(Some(Left(err))) =>
        val daoReadError = new Error(s"DynamoReadError: ${err.toString}")
        Left(StoreReadError(daoReadError))

      case Success(None) =>
        val error = new Throwable(s"There is no Dynamo item with id=$id")
        Left(DoesNotExistError(error))

      case Failure(err) => Left(StoreReadError(err))
    }
  }
}

trait DynamoHashReadable[HashKey, V, T]
    extends DynamoReadable[
      Version[HashKey, V],
      HashKey,
      DynamoHashEntry[HashKey, V, T],
      T] {
  implicit protected val formatHashKey: DynamoFormat[HashKey]

  protected def createKeyExpression(id: HashKey): UniqueKey[_] =
    "id" === id

  override def get(id: Version[HashKey, V]): ReadEither = {
    val storedEntry = getEntry(id.id)

    storedEntry.flatMap { entry =>
      if (entry.version == id.version) {
        Right(Identified(id, entry.payload))
      } else {
        Left(NoVersionExistsError(s"There is no Dynamo item with id=$id"))
      }
    }
  }
}

// TODO: Think of a better name than 'Version'
trait DynamoHashRangeReadable[HashKey, RangeKey, T]
    extends DynamoReadable[
      Version[HashKey, RangeKey],
      Version[HashKey, RangeKey],
      DynamoHashRangeEntry[HashKey, RangeKey, T],
      T] {

  implicit val formatHashKey: DynamoFormat[HashKey]
  implicit val formatRangeKey: DynamoFormat[RangeKey]

  protected def createKeyExpression(
    id: Version[HashKey, RangeKey]): UniqueKey[_] =
    "id" === id.id and "version" === id.version

  override def get(id: Version[HashKey, RangeKey]): ReadEither =
    getEntry(id).map { entry =>
      Identified(id, entry.payload)
    }
}
