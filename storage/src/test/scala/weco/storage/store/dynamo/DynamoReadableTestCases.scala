package weco.storage.store.dynamo

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import org.scanamo.{Table => ScanamoTable}
import org.scanamo.generic.auto._
import weco.storage.dynamo.DynamoEntry
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.{DoesNotExistError, Identified, Version}
import software.amazon.awssdk.services.dynamodb.model.{
  DynamoDbException,
  ResourceNotFoundException
}
trait DynamoReadableTestCases[
  DynamoIdent, EntryType <: DynamoEntry[String, Record]]
    extends AnyFunSpec
    with Matchers
    with DynamoFixtures
    with EitherValues
    with RecordGenerators {

  type HashKey = String

  type DynamoReadableStub =
    DynamoReadable[Version[HashKey, Int], DynamoIdent, EntryType, Record]

  // TODO: Make initialEntries an arbitrary type
  def createDynamoReadableWith(
    table: Table,
    initialEntries: Set[EntryType] = Set.empty): DynamoReadableStub

  def createEntry(hashKey: String, v: Int, record: Record): EntryType

  describe("DynamoReadable") {
    it("reads a row from the table") {
      val id = randomAlphanumeric()
      val record = createRecord

      val initialEntries = Set(
        createEntry(id, v = 1, record)
      )

      withLocalDynamoDbTable { table =>
        val readable = createDynamoReadableWith(table, initialEntries)

        readable.get(Version(id, 1)).value shouldBe Identified(
          Version(id, 1),
          record)
      }
    }

    it("finds nothing if the table is empty") {
      withLocalDynamoDbTable { table =>
        val readable = createDynamoReadableWith(table)

        readable
          .get(Version(randomAlphanumeric(), 1))
          .left
          .value shouldBe a[DoesNotExistError]
      }
    }

    it("finds nothing if there's no row with that hash key") {
      val id = randomAlphanumeric()

      val initialEntries = Set(
        createEntry(id, v = 1, createRecord)
      )

      withLocalDynamoDbTable { table =>
        val readable = createDynamoReadableWith(table, initialEntries)

        readable
          .get(Version(randomAlphanumeric(), 1))
          .left
          .value shouldBe a[DoesNotExistError]
      }
    }

    it("fails if DynamoDB has an error") {
      val readable = createDynamoReadableWith(nonExistentTable)

      val result = readable.get(Version(randomAlphanumeric(), 1))
      val err = result.left.value.e

      err shouldBe a[ResourceNotFoundException]
      err.getMessage should startWith(
        "Cannot do operations on a non-existent table")
    }

    it("fails if the row doesn't match the model") {
      // This doesn't have the payload field that our DynamoEntry model requires
      case class BadModel(id: String, version: Int, t: String)

      val id = randomAlphanumeric()

      withLocalDynamoDbTable { table =>
        scanamo.exec(
          ScanamoTable[BadModel](table.name).putAll(
            Set(BadModel(id, version = 1, t = randomAlphanumeric()))
          ))

        val readable = createDynamoReadableWith(table)

        val result = readable.get(Version(id, 1))
        val err = result.left.value.e

        err shouldBe a[Error]
        err.getMessage should startWith(
          "DynamoReadError: InvalidPropertiesError")
      }
    }
  }

  def assertErrorsOnWrongTableDefinition(createWrongTable: Table => Table,
                                         message: String): Assertion =
    withSpecifiedTable(createWrongTable) { table =>
      val readable = createDynamoReadableWith(table)

      val result = readable.get(id = Version(randomAlphanumeric(), 1))

      val err = result.left.value
      err.e shouldBe a[DynamoDbException]
      err.e.getMessage should startWith(message)
    }

  def assertErrorsOnBadKeyName(createWrongTable: Table => Table): Assertion =
    assertErrorsOnWrongTableDefinition(
      createWrongTable,
      message = "One of the required keys was not given a value")

  def assertErrorsOnBadKeyType(createWrongTable: Table => Table): Assertion =
    assertErrorsOnWrongTableDefinition(
      createWrongTable,
      message =
        "One or more parameter values were invalid: Type mismatch for key")
}
