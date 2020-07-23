package uk.ac.wellcome.storage.generators

import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container

trait AzureBlobLocationGenerators extends RandomThings {
  /** Create a valid container name, which means:
    *
    *   - all lowercase
    *   - between 3 and 63 characters long
    *   - only contains letters, numbers, and the dash (-) character
    *
    * See https://docs.microsoft.com/en-us/rest/api/storageservices/Naming-and-Referencing-Containers--Blobs--and-Metadata
    *
    */
  def createContainerName: String =
    randomAlphanumeric.toLowerCase

  def createContainer: Container =
    Container(randomAlphanumeric)

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

  def createAzureBlobLocationPrefix: AzureBlobLocationPrefix =
    createAzureBlobLocationPrefixWith(createContainer)
}
