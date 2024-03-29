package weco.storage.store.dynamo

import java.util.UUID

import weco.storage.store._
import weco.storage.store.s3.S3TypedStore
import weco.storage._
import weco.storage.providers.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

class DynamoHybridStore[T](prefix: S3ObjectLocationPrefix)(
  implicit val indexedStore: DynamoHashStore[String, Int, S3ObjectLocation],
  val typedStore: S3TypedStore[T]
) extends HybridStore[Version[String, Int], S3ObjectLocation, T] {

  override protected def createTypeStoreId(
    id: Version[String, Int]): S3ObjectLocation =
    prefix.asLocation(
      id.id,
      id.version.toString,
      UUID.randomUUID().toString + ".json")

  override protected def getTypedStoreEntry(typedStoreId: S3ObjectLocation)
    : Either[ReadError, Identified[S3ObjectLocation, T]] =
    super.getTypedStoreEntry(typedStoreId) match {
      case Right(t) => Right(t)
      case Left(err: StoreReadError)
          if err.e.getMessage.startsWith("The specified bucket is not valid") =>
        Left(DanglingHybridStorePointerError(err.e))
      case Left(err) => Left(err)
    }
}
