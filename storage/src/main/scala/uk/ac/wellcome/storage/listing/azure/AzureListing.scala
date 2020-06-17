package uk.ac.wellcome.storage.listing.azure

import uk.ac.wellcome.storage.ObjectLocationPrefix
import uk.ac.wellcome.storage.listing.Listing

trait AzureListing[Result] extends Listing[ObjectLocationPrefix, Result]


