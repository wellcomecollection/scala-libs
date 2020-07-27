package uk.ac.wellcome.storage.providers.memory

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MemoryLocationTest extends AnyFunSpec with Matchers {
  describe("MemoryLocation") {
    val loc = MemoryLocation(namespace = "my-memory-bukkit", path = "path/to/pictures")

    it("joins paths") {
      loc.join("cats", "devon-rex.jpg") shouldBe MemoryLocation(
        namespace = "my-memory-bukkit",
        path = "path/to/pictures/cats/devon-rex.jpg"
      )
    }

    it("creates a prefix") {
      loc.asPrefix shouldBe MemoryLocationPrefix(
        namespace = "my-memory-bukkit",
        path = "path/to/pictures"
      )
    }

    it("casts to a string") {
      loc.toString shouldBe "my-memory-bukkit/path/to/pictures"
    }
  }

  describe("MemoryLocationPrefix") {
    val prefix = MemoryLocationPrefix(
      namespace = "my-memory-bukkit",
      path = "path/to/different/pictures"
    )

    it("creates a location") {
      prefix.asLocation("dogs", "corgi.png") shouldBe MemoryLocation(
        namespace = "my-memory-bukkit",
        path = "path/to/different/pictures/dogs/corgi.png"
      )
    }

    it("gets the basename") {
      prefix.basename shouldBe "pictures"
    }

    it("gets the parent") {
      prefix.parent shouldBe MemoryLocationPrefix(
        namespace = "my-memory-bukkit",
        path = "path/to/different"
      )
    }

    it("casts to a string") {
      prefix.toString shouldBe "my-memory-bukkit/path/to/different/pictures"
    }
  }
}
