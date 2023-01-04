package weco.storage.s3

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.JsonUtil._
import weco.storage.generators.S3ObjectLocationGenerators
import org.scalatest.EitherValues

class S3ObjectLocationTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with S3ObjectLocationGenerators {

  import S3ObjectLocation._
  import S3ObjectLocationPrefix._

  describe("S3ObjectLocation") {
    describe("JSON") {
      it("can serialise and deserialise to JSON") {
        val location = createS3ObjectLocation

        val jsonString = toJson(location).get

        fromJson[S3ObjectLocation](jsonString).get shouldBe location
      }

      it("can serialise an old-style ObjectLocation from Json") {
        val jsonString =
          """{"namespace": "my-bukkit", "path": "path/to/my/key.txt"}"""

        val location = fromJson[S3ObjectLocation](jsonString).get

        location shouldBe S3ObjectLocation(
          bucket = "my-bukkit",
          key = "path/to/my/key.txt"
        )
      }
    }

    describe("behaves as a Location") {
      val loc =
        S3ObjectLocation(bucket = "my-s3-bucket", key = "path/to/pictures")

      it("joins paths") {
        loc.join("cats", "devon-rex.jpg") shouldBe S3ObjectLocation(
          bucket = "my-s3-bucket",
          key = "path/to/pictures/cats/devon-rex.jpg"
        )
      }

      it("removes double slashes when joining paths") {
        loc.join("trailing-slash/", "cornish-rex.jpg") shouldBe S3ObjectLocation(
          bucket = "my-s3-bucket",
          key = "path/to/pictures/trailing-slash/cornish-rex.jpg"
        )
      }

      it("creates a prefix") {
        loc.asPrefix shouldBe S3ObjectLocationPrefix(
          bucket = "my-s3-bucket",
          keyPrefix = "path/to/pictures"
        )
      }

      it("casts to a string") {
        loc.toString shouldBe "s3://my-s3-bucket/path/to/pictures"
      }

      it("blocks the . and .. characters in the object key") {
        val err1 = intercept[IllegalArgumentException] {
          S3ObjectLocation(bucket = "my-s3-bucket", key = "path/./to/pictures")
        }

        err1.getMessage shouldBe "requirement failed: S3 object key cannot contain '.' or '..' entries, or end in a trailing slash: path/./to/pictures"

        val err2 = intercept[IllegalArgumentException] {
          S3ObjectLocation(bucket = "my-s3-bucket", key = "path/../to/pictures")
        }

        err2.getMessage shouldBe "requirement failed: S3 object key cannot contain '.' or '..' entries, or end in a trailing slash: path/../to/pictures"
      }

      it("blocks multiple consecutive slashes") {
        val err = intercept[IllegalArgumentException] {
          S3ObjectLocation(bucket = "my-s3-bucket", key = "path//to/cat.jpg")
        }

        err.getMessage shouldBe "requirement failed: S3 object key cannot include multiple consecutive slashes: path//to/cat.jpg"
      }

      it("blocks a trailing slash") {
        val err = intercept[IllegalArgumentException] {
          S3ObjectLocation(bucket = "my-s3-bucket", key = "path/to/cat.jpg/")
        }

        err.getMessage shouldBe "requirement failed: S3 object key cannot end with a slash: path/to/cat.jpg/"
      }
    }
  }

  describe("S3ObjectLocationPrefix") {
    describe("JSON") {
      it("can serialise and deserialise to JSON") {
        val prefix = createS3ObjectLocationPrefix

        val jsonString = toJson(prefix).get

        fromJson[S3ObjectLocationPrefix](jsonString).get shouldBe prefix
      }

      it("can serialise an old-style ObjectLocationPrefix from Json") {
        val jsonString =
          """{"namespace": "my-bukkit", "path": "path/to/my/directory"}"""

        val prefix = fromJson[S3ObjectLocationPrefix](jsonString).get

        prefix shouldBe S3ObjectLocationPrefix(
          bucket = "my-bukkit",
          keyPrefix = "path/to/my/directory"
        )
      }
    }

    describe("behaves as a Prefix") {
      val prefix = S3ObjectLocationPrefix(
        bucket = "my-s3-bucket",
        keyPrefix = "path/to/different/pictures"
      )

      it("creates a location") {
        prefix.asLocation("dogs", "corgi.png") shouldBe S3ObjectLocation(
          bucket = "my-s3-bucket",
          key = "path/to/different/pictures/dogs/corgi.png"
        )
      }

      it("gets the basename") {
        prefix.basename shouldBe "pictures"
      }

      it("gets the parent") {
        prefix.parent shouldBe S3ObjectLocationPrefix(
          bucket = "my-s3-bucket",
          keyPrefix = "path/to/different"
        )
      }

      it("casts to a string") {
        prefix.toString shouldBe "s3://my-s3-bucket/path/to/different/pictures"
      }

      it("blocks the . and .. characters in the key prefix") {
        val err1 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(
            bucket = "my-s3-bucket",
            keyPrefix = "path/./to/pictures")
        }

        err1.getMessage shouldBe "requirement failed: S3 key prefix cannot contain '.' or '..' entries: path/./to/pictures"

        val err2 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(
            bucket = "my-s3-bucket",
            keyPrefix = "path/../to/pictures")
        }

        err2.getMessage shouldBe "requirement failed: S3 key prefix cannot contain '.' or '..' entries: path/../to/pictures"
      }

      it("blocks multiple consecutive slashes") {
        val err1 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(
            bucket = "my-s3-bucket",
            keyPrefix = "path/to/pictures//")
        }

        err1.getMessage shouldBe "requirement failed: S3 key prefix cannot include multiple consecutive slashes: path/to/pictures//"

        val err2 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(
            bucket = "my-s3-bucket",
            keyPrefix = "path//to/pictures")
        }

        err2.getMessage shouldBe "requirement failed: S3 key prefix cannot include multiple consecutive slashes: path//to/pictures"
      }

      it("allows a trailing slash") {
        S3ObjectLocationPrefix(
          bucket = "my-s3-bucket",
          keyPrefix = "path/to/pictures/")
      }
    }
  }
}
