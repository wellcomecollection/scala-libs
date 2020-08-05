package uk.ac.wellcome.storage.typesafe

import com.typesafe.config.Config
import uk.ac.wellcome.storage.listing.s3.{
  S3ObjectLocationListing,
  S3ObjectSummaryListing
}
import uk.ac.wellcome.storage.transfer.azure.{
  S3toAzurePrefixTransfer,
  S3toAzureTransfer
}
import uk.ac.wellcome.storage.transfer.s3.{S3PrefixTransfer, S3Transfer}
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object PrefixTransferBuilder {
  import CloudProvider._

  //  ## Expects HOCON config like:
  //  source.cloudProvider = "aws"
  //  destination.cloudProvider = "aws"
  //
  //  ## OR
  //  source.cloudProvider = "aws"
  //  destination {
  //    cloudProvider = "azure"
  //    azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
  //  }
  //

  def build(config: Config) = {
    val srcCloudProvider = config
      .getStringOption("source.cloudProvider")
      .flatMap(CloudProvider.create)
      .getOrElse(AWS)

    val dstCloudProvider = config
      .getStringOption("destination.cloudProvider")
      .flatMap(CloudProvider.create)
      .getOrElse(AWS)

    (srcCloudProvider, dstCloudProvider) match {
      case (AWS, AWS) => {
        implicit val s3Client = S3Builder.buildS3Client(config)
        implicit val summary = new S3ObjectSummaryListing()

        implicit val transfer = new S3Transfer()
        implicit val listing = new S3ObjectLocationListing()

        new S3PrefixTransfer()
      }
      case (AWS, Azure) => {
        val srcConfig = config.getConfig("source")
        val dstConfig = config.getConfig("destination")

        implicit val s3Client = S3Builder.buildS3Client(srcConfig)
        implicit val azureClient =
          AzureBlobServiceClientBuilder.build(dstConfig)

        implicit val s3toAzureTransfer: S3toAzureTransfer = S3toAzureTransfer()

        implicit val summary = new S3ObjectSummaryListing()
        implicit val listing = new S3ObjectLocationListing()

        new S3toAzurePrefixTransfer()
      }

      case _ =>
        throw new Exception(
          s"Invalid (source, destination) combination: ($srcCloudProvider, $dstCloudProvider)"
        )
    }
  }
}
