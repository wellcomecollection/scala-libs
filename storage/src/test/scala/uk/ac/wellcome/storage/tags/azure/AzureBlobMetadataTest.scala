package uk.ac.wellcome.storage.tags.azure

import java.util

import com.azure.storage.blob.models.BlobStorageException
import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.UpdateWriteError
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.tags.{Tags, TagsTestCases}

import scala.collection.JavaConverters._


class AzureBlobMetadataTest
  extends AnyFunSpec
    with Matchers
    with TagsTestCases[AzureBlobLocation, Container]
    with AzureFixtures {

  def putObject(location: AzureBlobLocation, metadata: Map[String, String] = Map.empty): Unit = {
    val streamLength = 256
    val allowOverwrites = true
    val inputStream = randomInputStream(length = streamLength)

    val individualBlobClient =
      azureClient
        .getBlobContainerClient(location.container)
        .getBlobClient(location.name)

    individualBlobClient.upload(
      inputStream,
      streamLength,
      allowOverwrites,
    )

    individualBlobClient
      .setMetadata(metadata.asJava)


  }

  override def withTags[R](initialTags: Map[AzureBlobLocation, Map[String, String]])(testWith: TestWith[Tags[AzureBlobLocation], R]): R = {
    initialTags
      .foreach { case (location, tags) =>
        putObject(
          location = location,
          metadata = tags
        )
      }

    testWith(new AzureBlobMetadata())
  }

  val azureBlobMetadata = new AzureBlobMetadata()

  override def createIdent(context: Container): AzureBlobLocation =
    createBlobLocationWith(context)

  override def withContext[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  it("throws an error if the tags are larger than 8KB in total") {
    withAzureContainer { container =>
      val location = createBlobLocationWith(container)
      putObject(location)

      val tooManyBytes =
        util.Arrays.toString(randomBytes(9000))

      val result =
        azureBlobMetadata
          .update(location) { existingTags: Map[String, String] =>
            Right(existingTags ++ Map("id" -> tooManyBytes))
          }

      assertIsBlobStoreException(result) {
        // Azurite does not seem to offer a useful status code
        _ should startWith("Status code 400")
      }
    }
  }

  private def assertIsBlobStoreException(result: azureBlobMetadata.UpdateEither)(assert: String => Assertion): Assertion = {
    val err = result.left.value

    err shouldBe a[UpdateWriteError]
    err.e shouldBe a[BlobStorageException]
    assert(err.e.getMessage)
  }
}
