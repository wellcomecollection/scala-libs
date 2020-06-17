package uk.ac.wellcome.storage.listing.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.{ObjectLocation, ObjectLocationPrefix}

class AzureObjectLocationListing(implicit itemListing: AzureBlobItemListing) extends AzureListing[ObjectLocation] {
  override def list(prefix: ObjectLocationPrefix): ListingResult =
    itemListing
      .list(prefix)
      .map { iterator =>
        iterator.map { item =>
          ObjectLocation(prefix.namespace, item.getName)
        }
      }
}

object AzureObjectLocationListing {
  def apply()(implicit blobClient: BlobServiceClient): AzureObjectLocationListing = {
    implicit val itemListing: AzureBlobItemListing =
      new AzureBlobItemListing()

    new AzureObjectLocationListing()
  }
}

