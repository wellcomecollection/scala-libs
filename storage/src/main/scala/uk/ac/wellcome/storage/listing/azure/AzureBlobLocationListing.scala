package uk.ac.wellcome.storage.listing.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}

class AzureBlobLocationListing(implicit itemListing: AzureBlobItemListing)
    extends AzureListing[AzureBlobLocation] {
  override def list(prefix: AzureBlobLocationPrefix): ListingResult =
    itemListing
      .list(prefix)
      .map { iterator =>
        iterator.map { item =>
          AzureBlobLocation(
            container = prefix.container,
            name = item.getName
          )
        }
      }
}

object AzureBlobLocationListing {
  def apply()(
    implicit blobClient: BlobServiceClient): AzureBlobLocationListing = {
    implicit val itemListing: AzureBlobItemListing =
      new AzureBlobItemListing()

    new AzureBlobLocationListing()
  }
}
