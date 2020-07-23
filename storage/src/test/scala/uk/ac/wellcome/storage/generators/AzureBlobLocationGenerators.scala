package uk.ac.wellcome.storage.generators

import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container

trait AzureBlobLocationGenerators extends RandomThings {
  def createAzureBlobLocationWith(container: Container): AzureBlobLocation =
    AzureBlobLocation(
      container = container.name,
      name = randomAlphanumeric
    )

  def createAzureBlobLocationPrefixWith(container: Container): AzureBlobLocationPrefix =
    AzureBlobLocationPrefix(
      container = container.name,
      namePrefix = randomAlphanumeric
    )
}
