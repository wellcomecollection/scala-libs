package uk.ac.wellcome.storage.typesafe

import software.amazon.awssdk.services.s3.AmazonS3
import com.typesafe.config.Config
import uk.ac.wellcome.storage.transfer.s3.S3PrefixTransfer
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
        implicit val s3Client: AmazonS3 = S3Builder.buildS3Client(config)

        S3PrefixTransfer()
      }

      case _ =>
        throw new Exception(
          s"Invalid (source, destination) combination: ($srcCloudProvider, $dstCloudProvider)"
        )
    }
  }
}
