package uk.ac.wellcome.storage.store.dynamo

import java.util.UUID

import uk.ac.wellcome.storage.{ObjectLocation, ObjectLocationPrefix, Version}
import uk.ac.wellcome.storage.store.{HybridStoreWithMaxima}
import uk.ac.wellcome.storage.store.s3.S3TypedStore

class DynamoHybridStoreWithMaxima[Id, V, T](prefix: ObjectLocationPrefix)(
  implicit val indexedStore: DynamoHashRangeStore[Id, V, ObjectLocation],
  val typedStore: S3TypedStore[T]
) extends HybridStoreWithMaxima[Id, V, ObjectLocation, T] {

  override protected def createTypeStoreId(id: Version[Id, V]): ObjectLocation =
    prefix.asLocation(
      id.id.toString,
      id.version.toString,
      UUID.randomUUID().toString)
}
