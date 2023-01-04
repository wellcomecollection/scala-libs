package weco.storage.transfer.azure

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.S3ObjectSummary
import software.amazon.awssdk.services.s3.S3Client
import weco.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import weco.storage.listing.Listing
import weco.storage.listing.s3.S3ObjectSummaryListing
import weco.storage.s3.S3ObjectLocationPrefix
import weco.storage.transfer.PrefixTransfer

class AzurePrefixTransfer(
  implicit
  s3Client: S3Client,
  val transfer: AzureTransfer[_]
) extends PrefixTransfer[
      S3ObjectLocationPrefix,
      S3ObjectSummary,
      AzureBlobLocationPrefix,
      AzureBlobLocation
    ] {

  override implicit val listing
    : Listing[S3ObjectLocationPrefix, S3ObjectSummary] =
    new S3ObjectSummaryListing()

  override protected def buildDstLocation(
    srcPrefix: S3ObjectLocationPrefix,
    dstPrefix: AzureBlobLocationPrefix,
    srcSummary: S3ObjectSummary
  ): AzureBlobLocation =
    dstPrefix.asLocation(
      srcSummary.getKey.stripPrefix(srcPrefix.keyPrefix)
    )
}
