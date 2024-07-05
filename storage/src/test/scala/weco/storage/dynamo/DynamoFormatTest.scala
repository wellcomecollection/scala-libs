package weco.storage.dynamo

import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import org.scanamo.generic.auto._
import org.scanamo.syntax._
import org.scanamo.{DynamoFormat, InvalidPropertiesError, Table => ScanamoTable}
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import DynamoTimeFormat._
trait DynamoFormatTestCases[T]
    extends AnyFunSpec
    with Matchers
    with DynamoFixtures
    with EitherValues {
  def createTable(table: Table): Table =
    createTableWithHashKey(table)

  def createT: T
  def createBadT: String

  implicit val dynamoFormat: DynamoFormat[T]

  case class RecordT(id: String, t: T)
  case class RecordTPlus(id: String, t: T, extra: Boolean)
  case class RecordTPlusDefault(id: String, t: T, extra: Boolean = true)

  it("allows storing and retrieving an instance of T") {
    val record = RecordT(id = "1", t = createT)

    withLocalDynamoDbTable { table =>
      val scanamoTable = ScanamoTable[RecordT](table.name)

      scanamo.exec(scanamoTable.put(record))
      scanamo
        .exec(scanamoTable.get("id" === record.id))
        .get
        .value shouldBe record
    }
  }

  it("errors if the field is the wrong type") {
    case class BadRecord(id: String, t: String)

    val record = BadRecord(id = "1", t = createBadT)

    withLocalDynamoDbTable { table =>
      scanamo.exec(ScanamoTable[BadRecord](table.name).put(record))

      val scanamoTable = ScanamoTable[RecordT](table.name)
      val err =
        scanamo.exec(scanamoTable.get("id" === record.id)).get.left.value
      err shouldBe a[InvalidPropertiesError]
    }
  }

  it("ignores extra fields") {
    val id = "2"
    val t = createT
    val record = RecordT(id = id, t = t)
    val recordPlus = RecordTPlus(id = id, t = t, extra = false)

    withLocalDynamoDbTable { table =>
      scanamo.exec(ScanamoTable[RecordTPlus](table.name).put(recordPlus))
      scanamo
        .exec(ScanamoTable[RecordT](table.name).get("id" === record.id))
        .get
        .value shouldBe record

    }
  }

  it("errors if fields are missing and there is no default value") {
    val record = RecordT(id = "3", t = createT)

    withLocalDynamoDbTable { table =>
      scanamo.exec(ScanamoTable[RecordT](table.name).put(record))

      val scanamoTable = ScanamoTable[RecordTPlus](table.name)
      val err =
        scanamo.exec(scanamoTable.get("id" === record.id)).get.left.value
      err shouldBe a[InvalidPropertiesError]
    }
  }

  it("uses default values if fields are missing") {
    val id = "4"
    val t = createT
    val record = RecordT(id = id, t = t)
    val recordTPlusDefault = RecordTPlusDefault(id = id, t = t)

    withLocalDynamoDbTable { table =>
      scanamo.exec(ScanamoTable[RecordT](table.name).put(record))
      scanamo
        .exec(
          ScanamoTable[RecordTPlusDefault](table.name).get("id" === record.id))
        .get
        .value shouldBe recordTPlusDefault
    }
  }
}

class DynamoInstantFormatTest extends DynamoFormatTestCases[Instant] {
  def createT: Instant =
    Instant
      .now()
      .truncatedTo(ChronoUnit.SECONDS)

  def createBadT: String = "not a valid datetime"

  implicit val dynamoFormat: DynamoFormat[Instant] = instantAsLongSecondsFormat
}

class DynamoURIFormatTest extends DynamoFormatTestCases[URI] {
  def createT: URI = new URI("https://example.org")

  def createBadT: String = "not a valid URI"

  implicit val dynamoFormat: DynamoFormat[URI] = uriDynamoFormat
}

class DynamoUUIDFormatTest extends DynamoFormatTestCases[UUID] {
  def createT: UUID = UUID.randomUUID()

  def createBadT: String = "not a valid UUID"

  implicit val dynamoFormat: DynamoFormat[UUID] = DynamoFormat.uuidFormat
}
