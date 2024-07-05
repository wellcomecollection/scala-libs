package weco.storage.maxima.dynamo

import org.scanamo.syntax._
import org.scanamo.{DynamoFormat, Scanamo, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import weco.storage.dynamo.DynamoHashRangeEntry
import weco.storage.maxima.Maxima
import weco.storage.{Identified, MaximaReadError, NoMaximaValueError, Version}

import scala.util.{Failure, Success, Try}

trait DynamoHashRangeMaxima[HashKey, RangeKey, T]
    extends Maxima[HashKey, Version[HashKey, RangeKey], T] {

  implicit protected val formatHashKey: DynamoFormat[HashKey]
  implicit protected val formatRangeKey: DynamoFormat[RangeKey]
  implicit protected val format: DynamoFormat[
    DynamoHashRangeEntry[HashKey, RangeKey, T]]

  protected val client: DynamoDbClient
  protected val table: Table[DynamoHashRangeEntry[HashKey, RangeKey, T]]

  override def max(hashKey: HashKey): MaxEither = {
    val ops = table.descending
      .limit(1)
      .query("id" === hashKey)

    Try(Scanamo(client).exec(ops)) match {
      case Success(List(Right(row))) =>
        Right(Identified(Version(row.hashKey, row.rangeKey), row.payload))
      case Success(List(Left(err))) =>
        val error = new Error(s"DynamoReadError: ${err.toString}")
        Left(MaximaReadError(error))
      case Success(Nil) =>
        val error = new Error(
          s"There are no Dynamo items with hash key id=$hashKey")
        Left(NoMaximaValueError(error))
      case Failure(err) => Left(MaximaReadError(err))

      // This case should be impossible to hit in practice -- limit(1)
      // means we should only get a single result from DynamoDB.
      case result =>
        val error = new Error(
          s"Unknown error from Scanamo! $result"
        )
        Left(MaximaReadError(error))
    }
  }
}
