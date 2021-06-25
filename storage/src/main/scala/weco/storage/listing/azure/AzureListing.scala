package weco.storage.listing.azure

import weco.storage.azure.AzureBlobLocationPrefix
import weco.storage.listing.Listing
import weco.storage.azure.AzureBlobLocationPrefix
import weco.storage.listing.Listing

trait AzureListing[Result] extends Listing[AzureBlobLocationPrefix, Result]
