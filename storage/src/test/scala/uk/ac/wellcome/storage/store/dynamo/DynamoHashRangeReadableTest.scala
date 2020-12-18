package uk.ac.wellcome.storage.store.dynamo

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import org.scanamo.{Table => ScanamoTable}
import org.scanamo.DynamoFormat
import uk.ac.wellcome.storage.{DoesNotExistError, Identified, Version}
import uk.ac.wellcome.storage.dynamo.DynamoHashRangeEntry
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.generators.Record
import org.scanamo.auto._

class DynamoHashRangeReadableTest
    extends DynamoReadableTestCases[
      Version[String, Int],
      DynamoHashRangeEntry[String, Int, Record]] {
  type HashRangeEntry = DynamoHashRangeEntry[String, Int, Record]

  override def createDynamoReadableWith(
    table: Table,
    initialEntries: Set[DynamoHashRangeEntry[String, Int, Record]])
    : DynamoReadableStub = {
    class DynamoHashRangeReadableImpl(
      val client: DynamoDbClient,
      val table: ScanamoTable[HashRangeEntry]
    )(
      implicit val formatHashKey: DynamoFormat[String],
      implicit val formatRangeKey: DynamoFormat[Int],
      implicit val format: DynamoFormat[HashRangeEntry]
    ) extends DynamoHashRangeReadable[HashKey, Int, Record]

    scanamo.exec(
      ScanamoTable[HashRangeEntry](table.name).putAll(initialEntries))

    new DynamoHashRangeReadableImpl(
      dynamoClient,
      ScanamoTable[HashRangeEntry](table.name))
  }

  override def createTable(table: Table): Table =
    createTableWithHashRangeKey(table)

  override def createEntry(hashKey: String,
                           v: Int,
                           record: Record): HashRangeEntry =
    DynamoHashRangeEntry(hashKey, v, record)

  describe("DynamoHashRangeReadable") {
    describe("it fails if the table has the wrong structure") {
      it("hash key name is wrong") {
        assertErrorsOnBadKeyName(
          table => createTableWithHashRangeKey(table, hashKeyName = "wrong")
        )
      }

      it("hash key is the wrong type") {
        assertErrorsOnBadKeyType(
          table =>
            createTableWithHashRangeKey(
              table,
              hashKeyType = ScalarAttributeType.N)
        )
      }

      it("range key name is wrong") {
        assertErrorsOnBadKeyName(
          table => createTableWithHashRangeKey(table, rangeKeyName = "wrong")
        )
      }

      it("range key is the wrong type") {
        assertErrorsOnBadKeyType(
          table =>
            createTableWithHashRangeKey(
              table,
              rangeKeyType = ScalarAttributeType.S)
        )
      }

      it("range key is missing") {
        assertErrorsOnWrongTableDefinition(
          table => createTableWithHashKey(table),
          message = "The number of conditions on the keys is invalid"
        )
      }
    }

    it("finds a row with matching hashKey and rangeKey") {
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

    it("fails if there's a row with matching hash but not range key") {
      val id = randomAlphanumeric()
      val record = createRecord

      val initialEntries = Set(
        createEntry(id, v = 2, record)
      )

      withLocalDynamoDbTable { table =>
        val readable = createDynamoReadableWith(table, initialEntries)

        readable.get(Version(id, 1)).left.value shouldBe a[DoesNotExistError]
      }
    }
  }
}
