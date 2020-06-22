package uk.ac.wellcome.storage.listing.azure

import org.scalatest.Assertion
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureBlobLocationPrefix}
import uk.ac.wellcome.storage.fixtures.AzureFixtures
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.listing.ListingTestCases

class AzureObjectLocationListingTest extends ListingTestCases[AzureBlobLocation, AzureBlobLocationPrefix, AzureBlobLocation, AzureObjectLocationListing, Container] with AzureFixtures {
  override def createIdent(implicit container: Container): AzureBlobLocation =
    createBlobLocationWith(container)

  override def extendIdent(location: AzureBlobLocation, extension: String): AzureBlobLocation =
    location.join(extension)

  override def createPrefix: AzureBlobLocationPrefix =
    AzureBlobLocationPrefix(
      container = createContainerName,
      namePrefix = randomAlphanumeric
    )

  override def createPrefixMatching(location: AzureBlobLocation): AzureBlobLocationPrefix =
    AzureBlobLocationPrefix(
      container = location.container,
      namePrefix = location.name
    )

  override def assertResultCorrect(result: Iterable[AzureBlobLocation], entries: Seq[AzureBlobLocation]): Assertion =
    result.toSeq should contain theSameElementsAs entries

  override def withListingContext[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  override def withListing[R](container: Container, initialEntries: Seq[AzureBlobLocation])(testWith: TestWith[AzureObjectLocationListing, R]): R = {
    initialEntries.foreach { location =>
      azureClient
        .getBlobContainerClient(location.container)
        .getBlobClient(location.name)
        .upload(randomInputStream(length = 20), 20)
    }

    testWith(
      AzureObjectLocationListing()
    )
  }
}
