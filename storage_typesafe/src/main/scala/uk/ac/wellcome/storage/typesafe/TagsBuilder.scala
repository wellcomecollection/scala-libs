package uk.ac.wellcome.storage.typesafe

import com.azure.storage.blob.{BlobServiceClient, BlobServiceClientBuilder}
import com.typesafe.config.Config
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.tags.Tags
import uk.ac.wellcome.storage.tags.azure.AzureBlobMetadata
import uk.ac.wellcome.storage.tags.s3.S3Tags
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object TagsBuilder {
  import CloudProvider._

  //  ## Expects HOCON config like:
  //  {
  //    cloudProvider = "aws"
  //    aws.s3.region = "eu-west-1"
  //  }
  //  ## OR
  //  {
  //    cloudProvider = "azure"
  //    azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
  //  }
  //

  def buildClient(config: Config)
    : Tags[ObjectLocation] with RetryableReadable[Map[String, String]] = {

    val cloudProvider = config
      .getStringOption("cloudProvider")
      .flatMap(CloudProvider.create)
      .getOrElse(AWS)

    cloudProvider match {
      case AWS => {
        val s3Client = S3Builder.buildS3Client(config)
        new S3Tags()(s3Client)
      }
      case Azure => {
        val connectionString = config
          .requireString("azure.blobStore.connectionString")

        val azureClient: BlobServiceClient =
          new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient()

        new AzureBlobMetadata()(azureClient)
      }
    }
  }
}
