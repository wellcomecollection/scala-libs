package weco.storage.store.dynamo

import org.scanamo.generic.auto._
import org.scanamo.{Table => ScanamoTable}
import weco.fixtures.TestWith
import weco.storage.Version
import weco.storage.dynamo.DynamoHashRangeEntry
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.store.StoreWithoutOverwritesTestCases
import weco.storage.dynamo.DynamoHashRangeEntry
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.store.StoreWithoutOverwritesTestCases

import scala.language.higherKinds

class DynamoHashRangeStoreTest
    extends StoreWithoutOverwritesTestCases[
      Version[String, Int],
      Record,
      String,
      Table]
    with RecordGenerators
    with DynamoFixtures {
  override def withStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record],
    table: Table)(testWith: TestWith[StoreImpl, R]): R = {
    val dynamoEntries = initialEntries.map {
      case (id, record) =>
        DynamoHashRangeEntry(id.id, id.version, record)
    }.toSet

    scanamo.exec(
      ScanamoTable[DynamoHashRangeEntry[String, Int, Record]](table.name)
        .putAll(dynamoEntries))

    val store = new DynamoHashRangeStore[String, Int, Record](
      config = createDynamoConfigWith(table)
    )

    testWith(store)
  }

  override def withStoreContext[R](testWith: TestWith[Table, R]): R =
    withLocalDynamoDbTable { table =>
      testWith(table)
    }

  override def createT: Record = createRecord

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createTable(table: Table): Table =
    createTableWithHashRangeKey(table)

  override def createId(implicit namespace: String): Version[String, Int] =
    Version(id = randomAlphanumeric(), version = 1)
}
