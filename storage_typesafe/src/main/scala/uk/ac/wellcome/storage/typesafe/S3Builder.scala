package uk.ac.wellcome.storage.typesafe

import software.amazon.awssdk.services.s3.AmazonS3
import com.typesafe.config.Config
import uk.ac.wellcome.config.models.AWSClientConfig
import uk.ac.wellcome.storage.s3._
import uk.ac.wellcome.typesafe.config.builders.AWSClientConfigBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object S3Builder extends AWSClientConfigBuilder {
  private def buildS3Client(awsClientConfig: AWSClientConfig): AmazonS3 =
    S3ClientFactory.create(
      region = awsClientConfig.region,
      endpoint = awsClientConfig.endpoint.getOrElse(""),
      accessKey = awsClientConfig.accessKey.getOrElse(""),
      secretKey = awsClientConfig.secretKey.getOrElse("")
    )

  def buildS3Client(config: Config): AmazonS3 =
    buildS3Client(
      awsClientConfig = buildAWSClientConfig(config, namespace = "s3")
    )

  def buildS3Config(config: Config, namespace: String = ""): S3Config = {
    val bucketName = config
      .requireString(s"aws.$namespace.s3.bucketName")

    S3Config(
      bucketName = bucketName
    )
  }
}
