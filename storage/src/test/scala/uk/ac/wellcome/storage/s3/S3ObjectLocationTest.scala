package uk.ac.wellcome.storage.s3

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
  }

  override def createTable(table: DynamoFixtures.Table): DynamoFixtures.Table =
    createTableWithHashKey(table)
}