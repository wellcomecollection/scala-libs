package weco.storage.store.dynamo

import org.scanamo.generic.auto._
import weco.fixtures.TestWith
import weco.storage.dynamo.DynamoConfig
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.fixtures.{DynamoFixtures, S3Fixtures}
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.s3.{S3ObjectLocation, S3ObjectLocationPrefix}
import weco.storage.store.s3.S3TypedStore
import weco.storage.store.VersionedStoreWithOverwriteTestCases
import weco.storage.{MaximaReadError, StoreReadError, StoreWriteError, Version}

import scala.language.higherKinds

class DynamoVersionedHybridStoreTest
    extends VersionedStoreWithOverwriteTestCases[
      String,
      Record,
      DynamoHybridStoreWithMaxima[String, Int, Record]]
    with RecordGenerators
    with S3Fixtures
    with DynamoFixtures {

  type DynamoVersionedStoreImpl =
    DynamoVersionedHybridStore[String, Int, Record]

  override def createTable(table: Table): Table =
    createTableWithHashRangeKey(table)

  override def withFailingGetVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R =
    withTypedStoreAndPrefix {
      case (typedStore, prefix) =>
        withIndexedStore { indexedStore =>
          val store =
            new DynamoHybridStoreWithMaxima[String, Int, Record](prefix)(
              indexedStore,
              typedStore) {
              override def max(id: String): MaxEither =
                Left(MaximaReadError(new Error("BOOM!")))
            }

          initialEntries.map {
            case (k, v) => store.put(k)(v).value
          }

          val vhs = new DynamoVersionedStoreImpl(store) {
            override def get(id: Version[String, Int]): ReadEither =
              Left(StoreReadError(new Error("BOOM!")))
          }

          testWith(vhs)
        }
    }

  override def withFailingPutVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    withVersionedStoreContext { storeContext =>
      initialEntries.map {
        case (k, v) => storeContext.put(k)(v).value
      }

      val vhs = new DynamoVersionedStoreImpl(storeContext) {
        override def put(id: Version[String, Int])(t: Record): WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }

      testWith(vhs)
    }
  }

  override def createIdent: String = randomAlphanumeric()

  override def createT: Record = createRecord

  override def withVersionedStoreImpl[R](
    initialEntries: Entries,
    storeContext: DynamoHybridStoreWithMaxima[String, Int, Record])(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    initialEntries.map {
      case (k, v) => storeContext.put(k)(v).value
    }

    testWith(new DynamoVersionedHybridStore[String, Int, Record](storeContext))
  }

  private def withTypedStoreAndPrefix[R](
    testWith: TestWith[(S3TypedStore[Record], S3ObjectLocationPrefix), R]
  ): R =
    withLocalS3Bucket { bucket =>
      val prefix = createS3ObjectLocationPrefixWith(bucket)

      testWith((S3TypedStore[Record], prefix))
    }

  private def withIndexedStore[R](
    testWith: TestWith[DynamoHashRangeStore[String, Int, S3ObjectLocation], R]
  ): R =
    withLocalDynamoDbTable { table =>
      val dynamoConfig = DynamoConfig(table.name, table.index)

      val indexedStore =
        new DynamoHashRangeStore[String, Int, S3ObjectLocation](dynamoConfig)

      testWith(indexedStore)
    }

  override def withVersionedStoreContext[R](
    testWith: TestWith[DynamoHybridStoreWithMaxima[String, Int, Record], R])
    : R =
    withTypedStoreAndPrefix {
      case (typedStore, prefix) =>
        withIndexedStore { indexedStore =>
          testWith(
            new DynamoHybridStoreWithMaxima[String, Int, Record](prefix)(
              indexedStore,
              typedStore))
        }
    }

  override def withStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record],
    storeContext: DynamoHybridStoreWithMaxima[String, Int, Record])(
    testWith: TestWith[StoreImpl, R]): R =
    withVersionedStoreImpl(initialEntries, storeContext)(testWith)

  override def withStoreContext[R](
    testWith: TestWith[DynamoHybridStoreWithMaxima[String, Int, Record], R])
    : R =
    withVersionedStoreContext(testWith)

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createId(implicit namespace: String): Version[String, Int] =
    Version(randomAlphanumeric(), 0)
}
