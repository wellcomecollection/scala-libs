package uk.ac.wellcome.storage.transfer.azure

import com.amazonaws.services.s3.AmazonS3
import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.listing.s3.S3ObjectLocationListing
import uk.ac.wellcome.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import uk.ac.wellcome.storage.transfer.PrefixTransfer

class S3toAzurePrefixTransfer()(
  implicit val transfer: S3toAzureTransfer,
  val listing: S3ObjectLocationListing
) extends PrefixTransfer[
      S3ObjectLocationPrefix,
      S3ObjectLocation,
      AzureBlobLocationPrefix,
      AzureBlobLocation
    ] {

  override protected def buildDstLocation(
    srcPrefix: S3ObjectLocationPrefix,
    dstPrefix: AzureBlobLocationPrefix,
    srcLocation: S3ObjectLocation): AzureBlobLocation =
    dstPrefix.asLocation(
      srcLocation.key.stripPrefix(srcPrefix.keyPrefix)
    )
}

object S3toAzurePrefixTransfer {
  def apply()(implicit
              s3Client: AmazonS3,
              blobClient: BlobServiceClient): S3toAzurePrefixTransfer = {
    implicit val transfer: S3toAzureTransfer = S3toAzureTransfer()
    implicit val listing: S3ObjectLocationListing = S3ObjectLocationListing()

    new S3toAzurePrefixTransfer()
  }
}
