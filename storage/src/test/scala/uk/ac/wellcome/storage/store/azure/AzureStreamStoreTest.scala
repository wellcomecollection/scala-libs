package uk.ac.wellcome.storage.store.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.generators.ObjectLocationGenerators
import uk.ac.wellcome.storage.store.StreamStoreTestCases
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

class AzureStreamStoreTest extends StreamStoreTestCases[ObjectLocation, Container, AzureStreamStore, Unit] with AzureFixtures with ObjectLocationGenerators {

  // Azurite test container does not error when handed incorrect stream lengths
  override lazy val skipStreamLengthTests = true

  override def withNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  override def createId(implicit container: Container): ObjectLocation =
    createObjectLocationWith(namespace = container.name)

  override def withStreamStoreImpl[R](context: Unit, initialEntries: Map[ObjectLocation, InputStreamWithLength])(
    testWith: TestWith[AzureStreamStore, R]): R = {
    initialEntries.foreach { case (location, data) =>
      azureClient
        .getBlobContainerClient(location.namespace)
        .getBlobClient(location.path)
        .upload(data, data.length)

      azureClient
        .getBlobContainerClient(location.namespace)
        .getBlobClient(location.path)
    }

    testWith(
      new AzureStreamStore()
    )
  }

  override def withStreamStoreContext[R](testWith: TestWith[Unit, R]): R =
    testWith(())
}
