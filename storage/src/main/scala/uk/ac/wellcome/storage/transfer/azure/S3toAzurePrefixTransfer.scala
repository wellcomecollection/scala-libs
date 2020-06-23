package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.storage.listing.s3.S3ObjectLocationListing
import uk.ac.wellcome.storage.transfer.ObjectLocationPrefixTransfer

class S3toAzurePrefixTransfer()(
  implicit val transfer: S3toAzureTransfer,
  val listing: S3ObjectLocationListing
) extends ObjectLocationPrefixTransfer
