package weco.storage.transfer.azure

import com.amazonaws.services.s3.AmazonS3
import weco.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import weco.storage.listing.Listing
import weco.storage.listing.s3.S3ObjectSummaryListing
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import weco.storage.transfer.PrefixTransfer

class AzurePrefixTransfer(
  implicit
  s3Client: AmazonS3,
  val transfer: AzureTransfer[_]
) extends PrefixTransfer[
      S3ObjectLocationPrefix,
      SourceS3Object,
      AzureBlobLocationPrefix,
      AzureBlobLocation
    ] {

  override implicit val listing
    : Listing[S3ObjectLocationPrefix, SourceS3Object] = {
    val underlying = new S3ObjectSummaryListing()

    new Listing[S3ObjectLocationPrefix, SourceS3Object] {
      override def list(prefix: S3ObjectLocationPrefix): ListingResult =
        underlying.list(prefix).map { iterable =>
          iterable.map { summary =>
            SourceS3Object(
              location = S3ObjectLocation(summary),
              size = summary.getSize
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
