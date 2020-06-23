package uk.ac.wellcome.storage.typesafe

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.storage.transfer.azure.S3toAzurePrefixTransfer

class PrefixTransferBuilderTest extends AnyFunSpec with Matchers {
  describe("with config for: source AWS, destination Azure") {
    val configString =
      """
        |source.cloudProvider = "aws"
        |destination {
        |   cloudProvider = "azure"
        |   azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
        |}
        |""".stripMargin

    it("creates a S3toAzurePrefixTransfer") {
      val config = ConfigFactory.parseString(configString)

      val prefixTransfer = PrefixTransferBuilder.build(config)

      prefixTransfer shouldBe a[S3toAzurePrefixTransfer]
    }
  }
}
