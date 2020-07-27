package uk.ac.wellcome.storage.azure

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AzureBlobLocationTest extends AnyFunSpec with Matchers {
  describe("AzureBlobLocation") {
    val loc = AzureBlobLocation(container = "my-azure-container", name = "path/to/pictures")

    it("joins paths") {
      loc.join("cats", "devon-rex.jpg") shouldBe AzureBlobLocation(
        container = "my-azure-container",
        name = "path/to/pictures/cats/devon-rex.jpg"
      )
    }

    it("creates a prefix") {
      loc.asPrefix shouldBe AzureBlobLocationPrefix(
        container = "my-azure-container",
        namePrefix = "path/to/pictures"
      )
    }

    it("casts to a string") {
      loc.toString shouldBe "azure://my-azure-container/path/to/pictures"
    }
  }

  describe("AzureBlobLocationPrefix") {
    val prefix = AzureBlobLocationPrefix(
      container = "my-azure-container",
      namePrefix = "path/to/different/pictures"
    )

    it("creates a location") {
      prefix.asLocation("dogs", "corgi.png") shouldBe AzureBlobLocation(
        container = "my-azure-container",
        name = "path/to/different/pictures/dogs/corgi.png"
      )
    }

    it("gets the basename") {
      prefix.basename shouldBe "pictures"
    }

    it("gets the parent") {
      prefix.parent shouldBe AzureBlobLocationPrefix(
        container = "my-azure-container",
        namePrefix = "path/to/different"
      )
    }

    it("casts to a string") {
      prefix.toString shouldBe "azure://my-azure-container/path/to/different/pictures"
    }
  }
}
