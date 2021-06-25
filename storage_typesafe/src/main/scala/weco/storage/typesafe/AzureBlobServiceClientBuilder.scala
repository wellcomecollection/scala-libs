package weco.storage.typesafe

import com.azure.storage.blob.{BlobServiceClient, BlobServiceClientBuilder}
import com.typesafe.config.Config
import weco.typesafe.config.builders.EnrichConfig._

object AzureBlobServiceClientBuilder {
  def build(config: Config): BlobServiceClient = {
    val connectionString = config
      .requireString("azure.blobStore.connectionString")

    new BlobServiceClientBuilder()
      .connectionString(connectionString)
      .buildClient()
  }
}
