package uk.ac.wellcome.storage.s3

import uk.ac.wellcome.json.JsonUtil._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.exceptions.JsonDecodingError

class S3ObjectLocationTest extends AnyFunSpec with Matchers {
  it("creates a location from a prefix") {
    val prefix = S3ObjectLocationPrefix(
      namespace = "my-test-bucket",
      path = "png-objects"
    )

    val location = prefix.asLocation("cats", "cat1.png")

    location shouldBe S3ObjectLocation(
      namespace = "my-test-bucket",
      path = "png-objects/cats/cat1.png"
    )
  }

  describe("JSON decoding of Location") {
    it("decodes from an old-style ObjectLocation") {
      val jsonString = """{"namespace": "my-bucket", "path": "myfile.txt"}"""

      val location = fromJson[S3ObjectLocation](jsonString).get

      location shouldBe S3ObjectLocation(
        namespace = "my-bucket",
        path = "myfile.txt"
      )
    }

    it("decodes from a new-style S3ObjectLocation") {
      val jsonString = """{"bucket": "my-bucket", "key": "myfile.txt"}"""

      val location = fromJson[S3ObjectLocation](jsonString).get

      location shouldBe S3ObjectLocation(
        namespace = "my-bucket",
        path = "myfile.txt"
      )
    }

    it("errors if the JSON is malformed") {
      val jsonString = """{"name": "lexie"}"""

      val result = fromJson[S3ObjectLocation](jsonString)

      result.failed.get shouldBe a[JsonDecodingError]
    }
  }

  describe("JSON decoding of Prefix") {
    it("decodes from an old-style ObjectLocationPrefix") {
      val jsonString = """{"namespace": "my-bucket", "path": "myfile.txt"}"""

      val location = fromJson[S3ObjectLocationPrefix](jsonString).get

      location shouldBe S3ObjectLocationPrefix(
        namespace = "my-bucket",
        path = "myfile.txt"
      )
    }

    it("decodes from a new-style S3ObjectLocationPrefix") {
      val jsonString = """{"bucket": "my-bucket", "key": "myfile.txt"}"""

      val location = fromJson[S3ObjectLocationPrefix](jsonString).get

      location shouldBe S3ObjectLocationPrefix(
        namespace = "my-bucket",
        path = "myfile.txt"
      )
    }

    it("errors if the JSON is malformed") {
      val jsonString = """{"name": "lexie"}"""

      val result = fromJson[S3ObjectLocationPrefix](jsonString)

      result.failed.get shouldBe a[JsonDecodingError]
    }
  }
}
