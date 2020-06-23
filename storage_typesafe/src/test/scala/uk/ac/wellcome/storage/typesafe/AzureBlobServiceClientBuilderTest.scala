package uk.ac.wellcome.storage.typesafe

import com.azure.storage.blob.BlobServiceClient
import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AzureBlobServiceClientBuilderTest extends AnyFunSpec with Matchers {
  describe("with config") {
    val configString =
      """
        |azure {
        |   blobStore.connectionString = "UseDevelopmentStorage=true;"
        |}
        |""".stripMargin

    it("creates AzureBlobServiceClient") {
      val config = ConfigFactory.parseString(configString)

      AzureBlobServiceClientBuilder.build(config) shouldBe a[BlobServiceClient]
    }
  }
}
