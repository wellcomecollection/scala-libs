package weco.storage.store.dynamo

import org.scanamo.DynamoFormat
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import weco.storage.dynamo.{DynamoConfig, DynamoHashEntry, DynamoHashRangeEntry}
import weco.storage.store.VersionedStore

class DynamoMultipleVersionStore[Id, T](val config: DynamoConfig)(
  implicit val client: DynamoDbClient,
  val formatHashKey: DynamoFormat[Id],
  val formatRangeKey: DynamoFormat[Int],
  val formatT: DynamoFormat[T],
  val formatDynamoHashRangeEntry: DynamoFormat[DynamoHashRangeEntry[Id, Int, T]]
) extends VersionedStore[Id, Int, T](
      new DynamoHashRangeStore[Id, Int, T](config)
    )

class DynamoSingleVersionStore[Id, T](val config: DynamoConfig)(
  implicit val client: DynamoDbClient,
  val formatHashKey: DynamoFormat[Id],
  val formatRangeKey: DynamoFormat[Int],
  val formatT: DynamoFormat[T],
  val formatDynamoHashEntry: DynamoFormat[DynamoHashEntry[Id, Int, T]]
) extends VersionedStore[Id, Int, T](
      new DynamoHashStore[Id, Int, T](config)
    )
