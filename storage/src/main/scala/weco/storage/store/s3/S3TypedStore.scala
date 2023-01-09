package weco.storage.store.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.transfer.s3.S3TransferManager
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.TypedStore
import weco.storage.streaming.Codec

class S3TypedStore[T](
  implicit val codec: Codec[T],
  val streamStore: S3StreamStore
) extends TypedStore[S3ObjectLocation, T]

object S3TypedStore {
  def apply[T](implicit codec: Codec[T],
               s3Client: S3Client,
               transferManager: S3TransferManager): S3TypedStore[T] = {
    implicit val streamStore: S3StreamStore = new S3StreamStore()

    new S3TypedStore[T]()
  }
}
