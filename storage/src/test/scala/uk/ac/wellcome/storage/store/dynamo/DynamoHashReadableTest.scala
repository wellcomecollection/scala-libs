package uk.ac.wellcome.storage.store.dynamo

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.{QueryRequest, ScalarAttributeType}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar
import org.scanamo.{DynamoFormat, Table => ScanamoTable}
import uk.ac.wellcome.storage.dynamo.DynamoHashEntry
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.generators.Record
import uk.ac.wellcome.storage.{Identified, NoVersionExistsError, Version}
import org.scanamo.auto._

class DynamoHashReadableTest
    extends DynamoReadableTestCases[
      String,
      DynamoHashEntry[String, Int, Record]]
    with MockitoSugar {
  type HashEntry = DynamoHashEntry[String, Int, Record]

  class DynamoHashReadableImpl(
    val client: AmazonDynamoDB,
    val table: ScanamoTable[HashEntry],
    override val consistencyMode: ConsistencyMode = EventuallyConsistent
  )(
    implicit val formatHashKey: DynamoFormat[String],
    implicit val format: DynamoFormat[HashEntry]
  ) extends DynamoHashReadable[HashKey, Int, Record]

  override def createDynamoReadableWith(
    table: Table,
    initialEntries: Set[DynamoHashEntry[String, Int, Record]] = Set.empty)
    : DynamoReadableStub = {
    scanamo.exec(ScanamoTable[HashEntry](table.name).putAll(initialEntries))

    new DynamoHashReadableImpl(
      dynamoClient,
      ScanamoTable[HashEntry](table.name))
  }

  override def createTable(table: Table): Table = createTableWithHashKey(table)

  override def createEntry(hashKey: String,
                           v: Int,
                           record: Record): HashEntry =
    DynamoHashEntry(hashKey, v, record)

  describe("DynamoHashReadable") {
    describe("fails if the table definition is wrong") {
      it("hash key name is wrong") {
        assertErrorsOnBadKeyName(
          table => createTableWithHashKey(table, keyName = "wrong"))
      }

      it("hash key is the wrong type") {
        assertErrorsOnBadKeyType(table =>
          createTableWithHashKey(table, keyType = ScalarAttributeType.N))
      }
    }

    it("finds a row with matching hashKey and version") {
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

    it("fails if there is a row with matching hashKey but wrong version") {
      val id = randomAlphanumeric()
      val record = createRecord

      val initialEntries = Set(
        createEntry(id, v = 2, record)
      )

      withLocalDynamoDbTable { table =>
        val readable = createDynamoReadableWith(table, initialEntries)

        readable
          .get(Version(id, 1))
          .left
          .value shouldBe a[NoVersionExistsError]
      }
    }
  }

  describe("observes the consistency setting") {
    it("the default is eventual consistency") {
      val mockClient = mock[AmazonDynamoDB]

      withLocalDynamoDbTable { table =>
        val readable = new DynamoHashReadableImpl(
          mockClient, ScanamoTable[HashEntry](table.name)
        )

        readable.consistencyMode shouldBe EventuallyConsistent

        readable.get(id = Version("b12345", version = 1))

        getConsistentReadOnQuery(mockClient) shouldBe false
      }
    }

    it("eventually consistent => consistent reads = false") {
      val mockClient = mock[AmazonDynamoDB]

      withLocalDynamoDbTable { table =>
        val readable = new DynamoHashReadableImpl(
          mockClient, ScanamoTable[HashEntry](table.name)
        ) {
          override val consistencyMode: ConsistencyMode = EventuallyConsistent
        }

        readable.get(id = Version("b12345", version = 1))

        getConsistentReadOnQuery(mockClient) shouldBe false
      }
    }

    it("strongly consistent => consistent reads = true") {
      val mockClient = mock[AmazonDynamoDB]

      withLocalDynamoDbTable { table =>
        val readable = new DynamoHashReadableImpl(
          mockClient, ScanamoTable[HashEntry](table.name)
        ) {
          override val consistencyMode: ConsistencyMode = StronglyConsistent
        }

        readable.get(id = Version("b12345", version = 1))

        getConsistentReadOnQuery(mockClient) shouldBe true
      }
    }

    def getConsistentReadOnQuery(mockClient: AmazonDynamoDB): Boolean = {
      val captor = ArgumentCaptor.forClass(classOf[QueryRequest])
      Mockito.verify(mockClient).query(captor.capture())
      captor.getValue.getConsistentRead
    }
  }
}
