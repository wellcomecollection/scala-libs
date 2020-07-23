package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.listing.s3.S3ObjectLocationListing
import uk.ac.wellcome.storage.transfer.PrefixTransfer
import uk.ac.wellcome.storage.{ObjectLocation, ObjectLocationPrefix}

class S3toAzurePrefixTransfer()(
  implicit val transfer: S3toAzureTransfer,
  val listing: S3ObjectLocationListing
) extends PrefixTransfer[
    ObjectLocationPrefix, ObjectLocation,
    AzureBlobLocationPrefix, AzureBlobLocation
  ] {

  override protected def buildDstLocation(
    srcPrefix: ObjectLocationPrefix,
    dstPrefix: AzureBlobLocationPrefix,
    srcLocation: ObjectLocation): AzureBlobLocation =
    dstPrefix.asLocation(
      srcLocation.path.stripPrefix(srcPrefix.path)
    )
}
