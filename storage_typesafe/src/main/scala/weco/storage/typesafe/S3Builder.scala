package weco.storage.typesafe

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.Config
import weco.storage.s3._
import weco.typesafe.config.builders.EnrichConfig._

object S3Builder {
  def buildS3Client: AmazonS3 =
    AmazonS3ClientBuilder.standard.build()

  def buildS3Config(config: Config, namespace: String = ""): S3Config = {
    val bucketName = config
      .requireString(s"aws.$namespace.s3.bucketName")

    S3Config(
      bucketName = bucketName
    )
  }
}
