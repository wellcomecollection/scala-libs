package uk.ac.wellcome.storage.tags.azure

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.tags.{Tags, TagsTestCases}
import scala.collection.JavaConverters._


class AzureBlobMetadataTest
  extends AnyFunSpec
    with Matchers
    with TagsTestCases[ObjectLocation, Container]
    with AzureFixtures {

  def putObject(location: ObjectLocation, metadata: Map[String, String] = Map.empty) = {
    val streamLength = 256
    val allowOverwrites = true
    val inputStream = randomInputStream(length = streamLength)

    val individualBlobClient =
      azureClient
        .getBlobContainerClient(location.namespace)
        .getBlobClient(location.path)

    individualBlobClient.upload(
      inputStream,
      streamLength,
      allowOverwrites,
    )

    individualBlobClient
      .setMetadata(metadata.asJava)


  }

  override def withTags[R](initialTags: Map[ObjectLocation, Map[String, String]])(testWith: TestWith[Tags[ObjectLocation], R]): R = {
    initialTags
      .foreach { case (location, tags) =>
        putObject(
          location = location,
          metadata = tags
        )
      }

    testWith(new AzureBlobMetadata())
  }

  override def createIdent(context: Container): ObjectLocation =
    createObjectLocationWith(context)

  override def withContext[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

}
