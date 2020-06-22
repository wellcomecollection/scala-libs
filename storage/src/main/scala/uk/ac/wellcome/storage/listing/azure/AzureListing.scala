package uk.ac.wellcome.storage.listing.azure

import uk.ac.wellcome.storage.azure.AzureBlobLocationPrefix
import uk.ac.wellcome.storage.listing.Listing

trait AzureListing[Result] extends Listing[AzureBlobLocationPrefix, Result]
