package weco.storage.transfer.azure

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.S3Object
import weco.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import weco.storage.listing.Listing
import weco.storage.listing.s3.S3ObjectListing
import weco.storage.s3.S3ObjectLocationPrefix
import weco.storage.transfer.PrefixTransfer

class AzurePrefixTransfer(
  implicit
  s3Client: S3Client,
  val transfer: AzureTransfer[_]
) extends PrefixTransfer[
      S3ObjectLocationPrefix,
      S3Object,
      AzureBlobLocationPrefix,
      AzureBlobLocation
    ] {

  override implicit val listing
    : Listing[S3ObjectLocationPrefix, S3Object] =
    new S3ObjectListing()

  override protected def buildDstLocation(
    srcPrefix: S3ObjectLocationPrefix,
    dstPrefix: AzureBlobLocationPrefix,
    srcSummary: S3Object
  ): AzureBlobLocation =
    dstPrefix.asLocation(
      srcSummary.key().stripPrefix(srcPrefix.keyPrefix)
    )
}
