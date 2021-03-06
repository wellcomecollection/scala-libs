package weco.storage.services.s3

import com.amazonaws.services.s3.AmazonS3
import weco.storage.s3.S3ObjectLocation
import weco.storage.services.{LargeStreamReader, RangedReader, SizeFinder}

class S3LargeStreamReader(val bufferSize: Long)(implicit s3Client: AmazonS3)
    extends LargeStreamReader[S3ObjectLocation] {
  override protected val sizeFinder: SizeFinder[S3ObjectLocation] =
    new S3SizeFinder()

  override protected val rangedReader: RangedReader[S3ObjectLocation] =
    new S3RangedReader()
}
