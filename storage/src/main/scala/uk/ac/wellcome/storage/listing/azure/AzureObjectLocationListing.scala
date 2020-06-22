package uk.ac.wellcome.storage.listing.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}

class AzureObjectLocationListing(implicit itemListing: AzureBlobItemListing)
    extends AzureListing[AzureBlobLocation] {
  override def list(prefix: AzureBlobLocationPrefix): ListingResult =
    itemListing
      .list(prefix)
      .map { iterator =>
        iterator.map { item =>
          AzureBlobLocation(prefix.container, item.getName)
        }
      }
}

object AzureObjectLocationListing {
  def apply()(
    implicit blobClient: BlobServiceClient): AzureObjectLocationListing = {
    implicit val itemListing: AzureBlobItemListing =
      new AzureBlobItemListing()

    new AzureObjectLocationListing()
  }
}
