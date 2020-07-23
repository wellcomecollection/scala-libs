package uk.ac.wellcome.storage.fixtures

import com.azure.storage.blob.{BlobServiceClient, BlobServiceClientBuilder}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import uk.ac.wellcome.fixtures.{Fixture, fixture}
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.generators.AzureBlobLocationGenerators

object AzureFixtures {
  class Container(val name: String) extends AnyVal {
    override def toString: String = s"AzureFixtures.Container($name)"
  }

  object Container {
    def apply(name: String): Container = new Container(name)
  }
}

trait AzureFixtures extends Eventually with IntegrationPatience with AzureBlobLocationGenerators {
  import AzureFixtures._

  implicit val azureClient: BlobServiceClient =
    new BlobServiceClientBuilder()
      .connectionString("UseDevelopmentStorage=true;")
      .buildClient()

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

  def createAzureObjectLocationWith(container: Container): ObjectLocation =
    ObjectLocation(
      namespace = container.name,
      path = randomAlphanumeric
    )

  def withAzureContainer[R]: Fixture[Container, R] =
    fixture[Container, R](
      create = {
        val containerName: String = createContainerName
        azureClient.createBlobContainer(containerName)

        Container(containerName)
      },
      destroy = { container: Container =>
        azureClient.deleteBlobContainer(container.name)
      }
    )
}
