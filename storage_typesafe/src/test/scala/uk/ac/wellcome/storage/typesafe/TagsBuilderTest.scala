package uk.ac.wellcome.storage.typesafe

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.storage.tags.azure.AzureBlobMetadata
import uk.ac.wellcome.storage.tags.s3.S3Tags


class TagsBuilderTest extends AnyFunSpec with Matchers {
  describe("with config for AWS") {
    val awsConfigString =
      """
        |somepath {
        |  cloudProvider = "aws"
        |  aws.s3.region = "eu-west-1"
        |}
        |""".stripMargin


    it("creates S3Tags") {
      val awsConfig = ConfigFactory.parseString(awsConfigString)
      val targetConfig = awsConfig.getConfig("somepath")
      val tags = TagsBuilder.buildClient(targetConfig)

      tags shouldBe a[S3Tags]
    }
  }

  describe("with config for Azure") {
    val azureConfigString =
      """
        |somepath {
        |  cloudProvider = "azure"
        |  azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
        |}
        |""".stripMargin


    it("creates AzureBlobMetadata") {
      val azureConfig = ConfigFactory.parseString(azureConfigString)
      val targetConfig = azureConfig.getConfig("somepath")
      val tags = TagsBuilder.buildClient(targetConfig)

      tags shouldBe a[AzureBlobMetadata]
    }
  }
}
