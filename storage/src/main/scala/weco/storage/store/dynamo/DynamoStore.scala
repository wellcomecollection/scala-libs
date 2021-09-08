package weco.storage.store.dynamo

import org.scanamo.{DynamoFormat, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import weco.storage._
import weco.storage.dynamo.{DynamoConfig, DynamoHashEntry, DynamoHashRangeEntry}
import weco.storage.maxima.Maxima
import weco.storage.maxima.dynamo.DynamoHashRangeMaxima
import weco.storage.store._

class DynamoHashRangeStore[HashKey, RangeKey, T](val config: DynamoConfig)(
  implicit val client: DynamoDbClient,
  val formatHashKey: DynamoFormat[HashKey],
  val formatRangeKey: DynamoFormat[RangeKey],
  val format: DynamoFormat[DynamoHashRangeEntry[HashKey, RangeKey, T]],
  override val consistencyMode: ConsistencyMode = EventuallyConsistent
) extends Store[Version[HashKey, RangeKey], T]
    with DynamoHashRangeReadable[HashKey, RangeKey, T]
    with DynamoHashRangeWritable[HashKey, RangeKey, T]
    with DynamoHashRangeMaxima[HashKey, RangeKey, T] {

  override protected val table =
    Table[DynamoHashRangeEntry[HashKey, RangeKey, T]](config.tableName)

}

class DynamoHashStore[HashKey, V, T](val config: DynamoConfig)(
  implicit val client: DynamoDbClient,
  val formatHashKey: DynamoFormat[HashKey],
  val formatV: DynamoFormat[V],
  val format: DynamoFormat[DynamoHashEntry[HashKey, V, T]],
  override val consistencyMode: ConsistencyMode = EventuallyConsistent
) extends Store[Version[HashKey, V], T]
    with DynamoHashReadable[HashKey, V, T]
    with DynamoHashWritable[HashKey, V, T]
    with Maxima[HashKey, Version[HashKey, V], T] {
  override def max(hashKey: HashKey): MaxEither =
    getEntry(hashKey) match {
      case Right(value) =>
        Right(Identified(Version(value.hashKey, value.version), value.payload))
      case Left(_: DoesNotExistError) =>
        val error = new Error(s"There are no Dynamo items with hash key id=$hashKey")
        Left(NoMaximaValueError(error))
      case Left(err: ReadError)       => Left(MaximaReadError(err.e))
    }

  override protected val table =
    Table[DynamoHashEntry[HashKey, V, T]](config.tableName)
}
