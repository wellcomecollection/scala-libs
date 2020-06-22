package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.listing.s3.S3ObjectLocationListing
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import uk.ac.wellcome.storage.transfer.PrefixTransfer

class S3toAzurePrefixTransfer()(
  implicit val transfer: S3toAzureTransfer,
  val listing: S3ObjectLocationListing
) extends PrefixTransfer[
      S3ObjectLocation,
      S3ObjectLocationPrefix,
      AzureBlobLocation,
      AzureBlobLocationPrefix] {
  override protected def buildDstLocation(
    srcPrefix: S3ObjectLocationPrefix,
    dstPrefix: AzureBlobLocationPrefix,
    srcLocation: S3ObjectLocation
  ): AzureBlobLocation =
    dstPrefix.asLocation(
      srcLocation.key.stripPrefix(srcPrefix.keyPrefix)
    )
}
