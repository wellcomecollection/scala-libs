package weco.storage.store.s3

import com.amazonaws.services.s3.AmazonS3
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.TypedStore
import weco.storage.streaming.Codec
import weco.storage.s3.S3ObjectLocation
import weco.storage.streaming.Codec

class S3TypedStore[T](
  implicit val codec: Codec[T],
  val streamStore: S3StreamStore
) extends TypedStore[S3ObjectLocation, T]

object S3TypedStore {
  def apply[T](implicit codec: Codec[T],
               s3Client: AmazonS3): S3TypedStore[T] = {
    implicit val streamStore: S3StreamStore = new S3StreamStore()

    new S3TypedStore[T]()
  }
}
