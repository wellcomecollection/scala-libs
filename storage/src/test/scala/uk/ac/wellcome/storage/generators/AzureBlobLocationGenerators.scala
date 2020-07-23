package uk.ac.wellcome.storage.generators

import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container

trait AzureBlobLocationGenerators extends RandomThings {
  def createAzureBlobLocationWith(container: Container): AzureBlobLocation =
    AzureBlobLocation(
      container = container.name,
      name = randomAlphanumeric
    )
}
