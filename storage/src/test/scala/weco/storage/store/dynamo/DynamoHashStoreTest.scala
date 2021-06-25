package weco.storage.store.dynamo

import weco.fixtures.TestWith
import org.scanamo.generic.auto._
import org.scanamo.{Table => ScanamoTable}
import weco.storage.{IdentityKey, Version}
import weco.storage.dynamo.DynamoHashEntry
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.maxima.MaximaTestCases
import weco.storage.store.StoreWithoutOverwritesTestCases
import weco.storage.dynamo.DynamoHashEntry
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.maxima.MaximaTestCases
import weco.storage.store.StoreWithoutOverwritesTestCases

import scala.language.higherKinds

class DynamoHashStoreTest
    extends StoreWithoutOverwritesTestCases[
      Version[IdentityKey, Int],
      Record,
      String,
      Table]
    with MaximaTestCases
    with RecordGenerators
    with DynamoFixtures {
  def withDynamoHashStore[R](
    initialEntries: Map[Version[IdentityKey, Int], Record],
    table: Table)(
    testWith: TestWith[DynamoHashStore[IdentityKey, Int, Record], R]): R = {
    val dynamoEntries = initialEntries.map {
      case (id, record) =>
        DynamoHashEntry(id.id, id.version, record)
    }.toSet

    dynamoEntries.foreach { entry =>
      scanamo.exec(
        ScanamoTable[DynamoHashEntry[IdentityKey, Int, Record]](table.name)
          .put(entry))
    }

    val store = new DynamoHashStore[IdentityKey, Int, Record](
      config = createDynamoConfigWith(table)
    )

    testWith(store)
  }

  override def withStoreImpl[R](
    initialEntries: Map[Version[IdentityKey, Int], Record],
    table: Table)(testWith: TestWith[StoreImpl, R]): R =
    withDynamoHashStore(initialEntries, table) { store =>
      testWith(store)
    }

  override def withStoreContext[R](testWith: TestWith[Table, R]): R =
    withLocalDynamoDbTable { table =>
      testWith(table)
    }

  override def createT: Record = createRecord

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createTable(table: Table): Table = createTableWithHashKey(table)

  override def createId(
    implicit namespace: String): Version[IdentityKey, Int] =
    Version(id = IdentityKey(randomAlphanumeric()), version = 1)

  override def withMaxima[R](
    initialEntries: Map[Version[IdentityKey, Int], Record])(
    testWith: TestWith[MaximaStub, R]): R =
    withLocalDynamoDbTable { table =>
      withDynamoHashStore(initialEntries, table) { store =>
        testWith(store)
      }
    }
}
