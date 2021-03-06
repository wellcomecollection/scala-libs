package weco.storage.store.dynamo

import org.scalatest.OptionValues
import org.scanamo.{DynamoFormat, Table => ScanamoTable}
import org.scanamo.generic.auto._
import org.scanamo.syntax._
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{
  DynamoDbException,
  ScalarAttributeType
}
import weco.storage.dynamo.DynamoHashRangeEntry
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.Version
import weco.storage.fixtures.DynamoFixtures.Table

import scala.language.higherKinds

class DynamoHashRangeWritableTest
    extends DynamoWritableTestCases[
      String,
      Record,
      DynamoHashRangeEntry[String, Int, Record]]
    with OptionValues
    with RecordGenerators {
  type HashRangeEntry = DynamoHashRangeEntry[String, Int, Record]

  def createId: String = randomAlphanumeric()
  def createT: Record = createRecord

  class HashRangeWritableImpl(
    val client: DynamoDbClient,
    val table: ScanamoTable[HashRangeEntry]
  )(
    implicit val formatRangeKey: DynamoFormat[Int]
  ) extends DynamoHashRangeWritable[String, Int, Record]

  override def createDynamoWritableWith(table: Table,
                                        initialEntries: Set[HashRangeEntry] =
                                          Set.empty): DynamoWritableStub = {
    scanamo.exec(
      ScanamoTable[HashRangeEntry](table.name).putAll(initialEntries))

    new HashRangeWritableImpl(
      dynamoClient,
      ScanamoTable[HashRangeEntry](table.name))
  }

  override def getT(table: Table)(hashKey: String, v: Int): Record =
    scanamo
      .exec(
        ScanamoTable[HashRangeEntry](table.name).get(
          "id" === hashKey and "version" === v)
      )
      .value
      .value
      .payload

  override def createEntry(hashKey: String,
                           v: Int,
                           record: Record): HashRangeEntry =
    DynamoHashRangeEntry(hashKey, v, record)

  override def createTable(table: Table): Table =
    createTableWithHashRangeKey(table)

  describe("DynamoHashRangeWritable") {

    it("allows putting the same hash key at multiple versions") {
      val hashKey = randomAlphanumeric()

      withLocalDynamoDbTable { table =>
        val writable = createDynamoWritableWith(
          table,
          initialEntries = Set(
            createEntry(hashKey, 2, createRecord)
          ))

        writable.put(id = Version(hashKey, 1))(createRecord) shouldBe a[
          Right[_, _]]
        writable.put(id = Version(hashKey, 3))(createRecord) shouldBe a[
          Right[_, _]]

        scanamo.exec(ScanamoTable[HashRangeEntry](table.name).scan()) should have size 3
      }
    }

    it("fails if the partition key is too long") {
      // Maximum length of an partition key is 2048 bytes as of 25/06/2019
      // https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Limits.html#limits-partition-sort-keys

      // There is also a sort key restriction of 1024 bytes
      // though as we use Int as our sort key this will be
      // impossible to reach as Int.MaxValue can be represented
      // in < 32 bits!

      val hashKey = randomStringOfByteLength(2049)

      val record = createRecord

      withLocalDynamoDbTable { table =>
        val writable =
          createDynamoWritableWith(table, initialEntries = Set.empty)
        val result = writable.put(id = Version(hashKey, 1))(record)

        val err = result.left.value

        err.e shouldBe a[DynamoDbException]
        err.e.getMessage should include(
          "Hash primary key values must be under 2048 bytes")
      }
    }

    describe("fails if the table definition is wrong") {
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
    }
  }
}
