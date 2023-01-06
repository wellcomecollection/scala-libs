package weco.storage.typesafe

import com.typesafe.config.Config
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.transfer.s3.S3TransferManager
import weco.storage.transfer.s3.S3PrefixTransfer
import weco.typesafe.config.builders.EnrichConfig._

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
      case (AWS, AWS) =>
        implicit val s3Client: S3Client = S3Client.builder().build()
        implicit val transferManager: S3TransferManager = S3TransferManager.builder().build()

        S3PrefixTransfer()

      case _ =>
        throw new Exception(
          s"Invalid (source, destination) combination: ($srcCloudProvider, $dstCloudProvider)"
        )
    }
  }
}
