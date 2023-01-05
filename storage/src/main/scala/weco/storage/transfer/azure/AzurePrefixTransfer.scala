package weco.storage.transfer.azure

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.S3Object
import weco.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import weco.storage.listing.Listing
import weco.storage.listing.s3.S3ObjectListing
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import weco.storage.transfer.PrefixTransfer

class AzurePrefixTransfer(
  implicit
  s3Client: S3Client,
  val transfer: AzureTransfer[_]
) extends PrefixTransfer[
      S3ObjectLocationPrefix,
      SourceS3Object,
      AzureBlobLocationPrefix,
      AzureBlobLocation
    ] {

  override implicit val listing
    : Listing[S3ObjectLocationPrefix, SourceS3Object] = {
    val underlying = new S3ObjectListing()

    new Listing[S3ObjectLocationPrefix, SourceS3Object] {
      override def list(prefix: S3ObjectLocationPrefix): ListingResult =
        underlying.list(prefix).map { iterable =>
          iterable.map { s3Obj: S3Object =>
            SourceS3Object(
              location =
                S3ObjectLocation(bucket = prefix.bucket, key = s3Obj.key()),
              size = s3Obj.size()
            )
          }
        }
    }
  }

  override protected def buildDstLocation(
    srcPrefix: S3ObjectLocationPrefix,
    dstPrefix: AzureBlobLocationPrefix,
    srcSummary: SourceS3Object
  ): AzureBlobLocation =
    dstPrefix.asLocation(
      srcSummary.location.key.stripPrefix(srcPrefix.keyPrefix)
    )
}
