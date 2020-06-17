package uk.ac.wellcome.storage.store.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.{ObjectLocation, OverwriteError}
import uk.ac.wellcome.storage.fixtures.AzureFixtures
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.generators.ObjectLocationGenerators
import uk.ac.wellcome.storage.store.StreamStoreTestCases
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

case class AzureStreamStoreContext(
  allowOverwrite: Option[Boolean] = None
)

class AzureStreamStoreTest
    extends StreamStoreTestCases[
      ObjectLocation,
      Container,
      AzureStreamStore,
      AzureStreamStoreContext]
    with AzureFixtures
    with ObjectLocationGenerators {

  // Azurite test container does not error when handed incorrect stream lengths
  override lazy val skipStreamLengthTests = true

  override def withNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  override def createId(implicit container: Container): ObjectLocation =
    createObjectLocationWith(namespace = container.name)

  override def withStreamStoreImpl[R](
    context: AzureStreamStoreContext,
    initialEntries: Map[ObjectLocation, InputStreamWithLength])(
    testWith: TestWith[AzureStreamStore, R]): R = {
    initialEntries.foreach {
      case (location, data) =>
        azureClient
          .getBlobContainerClient(location.namespace)
          .getBlobClient(location.path)
          .upload(data, data.length)

        azureClient
          .getBlobContainerClient(location.namespace)
          .getBlobClient(location.path)
    }

    val store = context.allowOverwrite.map { allowOverwrites =>
      new AzureStreamStore(allowOverwrites = allowOverwrites)
    } getOrElse (new AzureStreamStore())

    testWith(store)
  }

  override def withStreamStoreContext[R](
    testWith: TestWith[AzureStreamStoreContext, R]): R =
    testWith(AzureStreamStoreContext())

  describe("allowOverwrites is false") {

    it("will not overwrite an existing object") {
      withNamespace { implicit namespace =>
        val id = createId
        val entry = ReplayableStream(randomBytes(256))
        val updatedEntry = ReplayableStream(randomBytes(256))

        withStoreImpl(
          initialEntries = Map(id -> entry),
          storeContext = AzureStreamStoreContext(
            allowOverwrite = Some(false)
          )
        ) { store =>
          val result = store.put(id)(updatedEntry).left.value

          result shouldBe a[OverwriteError]
        }
      }
    }
  }
}