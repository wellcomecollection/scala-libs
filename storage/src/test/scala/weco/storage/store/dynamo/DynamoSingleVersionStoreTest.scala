package weco.storage.store.dynamo

import org.scanamo.{Table => ScanamoTable}
import org.scanamo.generic.auto._
import weco.fixtures.TestWith
import weco.storage.dynamo.DynamoHashEntry
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.store._
import weco.storage.{MaximaReadError, StoreReadError, StoreWriteError, Version}
import weco.storage.fixtures.DynamoFixtures
import weco.storage.fixtures.DynamoFixtures.Table

import scala.language.higherKinds

class DynamoSingleVersionStoreTest
    extends VersionedStoreWithOverwriteTestCases[String, Record, Table]
    with RecordGenerators
    with DynamoFixtures {

  override def createIdent: String = randomAlphanumeric()
  override def createT: Record = createRecord

  type DynamoStoreStub = DynamoSingleVersionStore[String, Record]

  override def createTable(table: Table): Table =
    createTableWithHashKey(table)

  private def insertEntries(table: Table)(
    entries: Map[Version[String, Int], Record]): Unit = {
    val scanamoTable =
      new ScanamoTable[DynamoHashEntry[String, Int, Record]](table.name)

    val rows = entries.map {
      case (Version(id, version), payload) =>
        DynamoHashEntry(id, version, payload)
    }

    debug(s"Inserting rows: $rows")

    scanamo.exec(scanamoTable.putAll(rows.toSet))
  }

  override def withVersionedStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record])(
    testWith: TestWith[VersionedStoreImpl, R]): R =
    withLocalDynamoDbTable { table =>
      val store = new DynamoStoreStub(
        config = createDynamoConfigWith(table)
      )

      insertEntries(table)(initialEntries)

      testWith(store)
    }

  override def withVersionedStoreImpl[R](
    initialEntries: Entries,
    storeContext: Table)(testWith: TestWith[VersionedStoreImpl, R]): R = {
    val store = new DynamoStoreStub(
      config = createDynamoConfigWith(storeContext)
    )

    insertEntries(storeContext)(initialEntries)

    testWith(store)
  }

  override def withVersionedStoreContext[R](testWith: TestWith[Table, R]): R =
    withLocalDynamoDbTable { table =>
      testWith(table)
    }

  override def withFailingGetVersionedStore[R](
    initialEntries: Map[Version[String, Int], Record])(
    testWith: TestWith[VersionedStoreImpl, R]): R =
    withLocalDynamoDbTable { table =>
      val config = createDynamoConfigWith(table)

      val underlying =
        new DynamoHashStore[String, Int, Record](config) {
          override def max(hashKey: String): MaxEither =
            Left(MaximaReadError(new Error("BOOM!")))
        }

      val store = new DynamoStoreStub(config) {
        override def get(id: Version[String, Int]): ReadEither = {
          Left(StoreReadError(new Error("BOOM!")))
        }

        override val store: DynamoHashStore[String, Int, Record] = underlying
      }

      insertEntries(table)(initialEntries)

      testWith(store)
    }

  override def withFailingPutVersionedStore[R](
    initialEntries: Map[Version[String, Int], Record])(
    testWith: TestWith[VersionedStoreImpl, R]): R =
    withLocalDynamoDbTable { table =>
      val store = new DynamoStoreStub(
        config = createDynamoConfigWith(table)
      ) {
        override def put(id: Version[String, Int])(t: Record): WriteEither = {
          Left(StoreWriteError(new Error("BOOM!")))
        }
      }

      insertEntries(table)(initialEntries)

      testWith(store)
    }

  override def withStoreContext[R](testWith: TestWith[Table, R]): R =
    withVersionedStoreContext(testWith)

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createId(implicit namespace: String): Version[String, Int] =
    Version(randomAlphanumeric(), 0)

  override def withStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record],
    storeContext: Table)(testWith: TestWith[StoreImpl, R]): R =
    withVersionedStoreImpl(initialEntries, storeContext)(testWith)

}
