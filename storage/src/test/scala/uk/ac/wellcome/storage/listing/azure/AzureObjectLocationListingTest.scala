package uk.ac.wellcome.storage.listing.azure

import org.scalatest.Assertion
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.AzureFixtures
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.{ObjectLocation, ObjectLocationPrefix}
import uk.ac.wellcome.storage.listing.ListingTestCases

class AzureObjectLocationListingTest extends ListingTestCases[ObjectLocation, ObjectLocationPrefix, ObjectLocation, AzureObjectLocationListing, Container] with AzureFixtures {
  override def createIdent(implicit container: Container): ObjectLocation =
    createObjectLocationWith(namespace = container.name)

  override def extendIdent(location: ObjectLocation, extension: String): ObjectLocation =
    location.join(extension)

  override def createPrefix: ObjectLocationPrefix =
    createObjectLocationPrefixWith(namespace = createContainerName)

  override def createPrefixMatching(location: ObjectLocation): ObjectLocationPrefix =
    location.asPrefix

  override def assertResultCorrect(result: Iterable[ObjectLocation], entries: Seq[ObjectLocation]): Assertion =
    result.toSeq should contain theSameElementsAs entries

  override def withListingContext[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  override def withListing[R](container: Container, initialEntries: Seq[ObjectLocation])(testWith: TestWith[AzureObjectLocationListing, R]): R = {
    initialEntries.foreach { location =>
      azureClient
        .getBlobContainerClient(location.namespace)
        .getBlobClient(location.path)
        .upload(randomInputStream(length = 20), 20)
    }

    testWith(
      AzureObjectLocationListing()
    )
  }
}
