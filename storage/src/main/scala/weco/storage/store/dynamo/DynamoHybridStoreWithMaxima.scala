package weco.storage.store.dynamo

import java.util.UUID

import weco.storage.Version
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import weco.storage.store.HybridStoreWithMaxima
import weco.storage.store.s3.S3TypedStore

class DynamoHybridStoreWithMaxima[Id, V, T](prefix: S3ObjectLocationPrefix)(
  implicit val indexedStore: DynamoHashRangeStore[Id, V, S3ObjectLocation],
  val typedStore: S3TypedStore[T]
) extends HybridStoreWithMaxima[Id, V, S3ObjectLocation, T] {

  override protected def createTypeStoreId(
    id: Version[Id, V]): S3ObjectLocation =
    prefix.asLocation(
      id.id.toString,
      id.version.toString,
      UUID.randomUUID().toString + ".json")
}
