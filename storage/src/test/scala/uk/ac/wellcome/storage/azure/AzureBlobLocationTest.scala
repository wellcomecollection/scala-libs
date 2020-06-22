package uk.ac.wellcome.storage.azure

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AzureBlobLocationTest extends AnyFunSpec with Matchers {
  it("creates a location from a prefix") {
    val prefix = AzureBlobLocationPrefix(
      container = "my-test-container",
      namePrefix = "jpeg-blobs"
    )

    val location = prefix.asLocation("cats", "cat1.jpg")

    location shouldBe AzureBlobLocation(
      container = "my-test-container",
      name = "jpeg-blobs/cats/cat1.jpg"
    )
  }
}
