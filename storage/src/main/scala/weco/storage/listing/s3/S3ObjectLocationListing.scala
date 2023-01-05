package weco.storage.listing.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.S3Object
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}

class S3ObjectLocationListing(implicit summaryListing: S3ObjectListing)
    extends S3Listing[S3ObjectLocation] {
  override def list(prefix: S3ObjectLocationPrefix): ListingResult =
    summaryListing.list(prefix) match {
      case Right(result) =>
        Right(result.map { s3Obj: S3Object =>
          S3ObjectLocation(bucket = prefix.bucket, key = s3Obj.key())
        })
      case Left(err) => Left(err)
    }
}

object S3ObjectLocationListing {
  def apply()(implicit s3Client: S3Client): S3ObjectLocationListing = {
    implicit val summaryListing: S3ObjectListing =
      new S3ObjectListing()

    new S3ObjectLocationListing()
  }
}
