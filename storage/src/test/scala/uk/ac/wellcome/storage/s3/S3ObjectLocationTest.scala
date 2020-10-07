package uk.ac.wellcome.storage.s3

import com.amazonaws.services.s3.model.S3ObjectSummary
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scanamo.auto._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.generators.S3ObjectLocationGenerators
import org.scalatest.EitherValues
import uk.ac.wellcome.storage.fixtures.DynamoFixtures

class S3ObjectLocationTest
  extends AnyFunSpec
    with Matchers
    with EitherValues
    with DynamoFixtures
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
        val jsonString = """{"namespace": "my-bukkit", "path": "path/to/my/key.txt"}"""

        val location = fromJson[S3ObjectLocation](jsonString).get

        location shouldBe S3ObjectLocation(
          bucket = "my-bukkit",
          key = "path/to/my/key.txt"
        )
      }
    }

    describe("DynamoDB") {
      case class IdentifiedLocation(
        id: String, location: S3ObjectLocation
      )

      val item = IdentifiedLocation(
        id = "cat", location = S3ObjectLocation(bucket = "my-bukkit", key = "cat.jpg")
      )

      it("can store a location in DynamoDB") {
        withLocalDynamoDbTable { table =>
          putTableItem(table = table, item = item)

          getTableItem[IdentifiedLocation](id = item.id, table = table).get.right.value shouldBe item
        }
      }

      it("can retrieve an old-style location from DynamoDB") {
        case class OldLocation(namespace: String, path: String)
        case class OldItem(id: String, location: OldLocation)

        val oldItem = OldItem(
          id = "cat",
          location = OldLocation(namespace = "my-bukkit", path = "cat.jpg")
        )

        withLocalDynamoDbTable { table =>
          putTableItem(table = table, item = oldItem)

          getTableItem[IdentifiedLocation](id = item.id, table = table).get.right.value shouldBe item
        }
      }
    }

    describe("behaves as a Location") {
      val loc = S3ObjectLocation(bucket = "my-s3-bucket", key = "path/to/pictures")

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

    it("can be created from an S3ObjectSummary") {
      val bucket = createBucketName
      val key = randomAlphanumeric()

      val summary = new S3ObjectSummary()
      summary.setBucketName(bucket)
      summary.setKey(key)
      
      S3ObjectLocation(summary) shouldBe S3ObjectLocation(bucket, key)
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
        val jsonString = """{"namespace": "my-bukkit", "path": "path/to/my/directory"}"""

        val prefix = fromJson[S3ObjectLocationPrefix](jsonString).get

        prefix shouldBe S3ObjectLocationPrefix(
          bucket = "my-bukkit",
          keyPrefix = "path/to/my/directory"
        )
      }
    }

    describe("DynamoDB") {
      case class IdentifiedLocation(
        id: String, location: S3ObjectLocationPrefix
      )

      val item = IdentifiedLocation(
        id = "cats", location = S3ObjectLocationPrefix(bucket = "my-bukkit", keyPrefix = "dir/of/cats")
      )

      it("can store a location in DynamoDB") {
        withLocalDynamoDbTable { table =>
          putTableItem(table = table, item = item)

          getTableItem[IdentifiedLocation](id = item.id, table = table).get.right.value shouldBe item
        }
      }

      it("can retrieve an old-style location from DynamoDB") {
        case class OldPrefix(namespace: String, path: String)
        case class OldItem(id: String, location: OldPrefix)

        val oldItem = OldItem(
          id = "cats",
          location = OldPrefix(namespace = "my-bukkit", path = "dir/of/cats")
        )

        withLocalDynamoDbTable { table =>
          putTableItem(table = table, item = oldItem)

          getTableItem[IdentifiedLocation](id = item.id, table = table).get.right.value shouldBe item
        }
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
          S3ObjectLocationPrefix(bucket = "my-s3-bucket", keyPrefix = "path/./to/pictures")
        }

        err1.getMessage shouldBe "requirement failed: S3 key prefix cannot contain '.' or '..' entries: path/./to/pictures"

        val err2 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(bucket = "my-s3-bucket", keyPrefix = "path/../to/pictures")
        }

        err2.getMessage shouldBe "requirement failed: S3 key prefix cannot contain '.' or '..' entries: path/../to/pictures"
      }

      it("blocks multiple consecutive slashes") {
        val err1 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(bucket = "my-s3-bucket", keyPrefix = "path/to/pictures//")
        }

        err1.getMessage shouldBe "requirement failed: S3 key prefix cannot include multiple consecutive slashes: path/to/pictures//"

        val err2 = intercept[IllegalArgumentException] {
          S3ObjectLocationPrefix(bucket = "my-s3-bucket", keyPrefix = "path//to/pictures")
        }

        err2.getMessage shouldBe "requirement failed: S3 key prefix cannot include multiple consecutive slashes: path//to/pictures"
      }

      it("allows a trailing slash") {
        S3ObjectLocationPrefix(bucket = "my-s3-bucket", keyPrefix = "path/to/pictures/")
      }
    }
  }

  override def createTable(table: DynamoFixtures.Table): DynamoFixtures.Table =
    createTableWithHashKey(table)
}
