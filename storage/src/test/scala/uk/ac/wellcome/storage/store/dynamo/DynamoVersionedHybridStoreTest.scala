package uk.ac.wellcome.storage.store.dynamo

import org.scanamo.auto._
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.fixtures.{DynamoFixtures, S3Fixtures}
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.s3.{S3StreamStore, S3TypedStore}
import uk.ac.wellcome.storage.store.VersionedStoreWithOverwriteTestCases
import uk.ac.wellcome.storage.{StoreReadError, StoreWriteError, Version}

class DynamoVersionedHybridStoreTest
    extends VersionedStoreWithOverwriteTestCases[
      String,
      Record,
      DynamoHybridStoreWithMaxima[String, Int, Record]]
    with RecordGenerators
    with S3Fixtures
    with DynamoFixtures {

  type DynamoVersionedStoreImpl = DynamoVersionedHybridStore[String, Int, Record]

  override def createTable(table: Table): Table =
    createTableWithHashRangeKey(table)

  override def withFailingGetVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    withVersionedStoreContext { storeContext =>
      initialEntries.map {
        case (k, v) => storeContext.put(k)(v).right.value
      }

      val vhs = new DynamoVersionedStoreImpl(storeContext) {
        override def get(id: Version[String, Int]): ReadEither = {
          Left(StoreReadError(new Error("BOOM!")))
        }
      }

      testWith(vhs)
    }
  }

  override def withFailingPutVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    withVersionedStoreContext { storeContext =>
      initialEntries.map {
        case (k, v) => storeContext.put(k)(v).right.value
      }

      val vhs = new DynamoVersionedStoreImpl(storeContext) {
        override def put(id: Version[String, Int])(t: Record): WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }

      testWith(vhs)
    }
  }

  override def createIdent: String = randomAlphanumeric

  override def createT: Record = createRecord

  override def withVersionedStoreImpl[R](
    initialEntries: Entries,
    storeContext: DynamoHybridStoreWithMaxima[String,
                                              Int,
                                              Record])(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    initialEntries.map {
      case (k, v) => storeContext.put(k)(v).right.value
    }

    testWith(
      new DynamoVersionedHybridStore[String, Int, Record](
        storeContext))
  }

  override def withVersionedStoreContext[R](
    testWith: TestWith[DynamoHybridStoreWithMaxima[String, Int, Record], R]): R =
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        val streamStore = new S3StreamStore()
        val typedStore = new S3TypedStore[Record]()(codec, streamStore)

        val dynamoConfig = DynamoConfig(table.name, table.index)

        val indexedStore =
          new DynamoHashRangeStore[String, Int, S3ObjectLocation](
            dynamoConfig)

        val prefix = createS3ObjectLocationPrefixWith(bucket)

        testWith(
          new DynamoHybridStoreWithMaxima[
            String,
            Int,
            Record](prefix)(indexedStore, typedStore))
      }
    }

  override def withStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record],
    storeContext: DynamoHybridStoreWithMaxima[String,
                                              Int,
                                              Record])(
    testWith: TestWith[StoreImpl, R]): R =
    withVersionedStoreImpl(initialEntries, storeContext)(testWith)

  override def withStoreContext[R](
    testWith: TestWith[
      DynamoHybridStoreWithMaxima[String, Int, Record],
      R]): R =
    withVersionedStoreContext(testWith)

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric)

  override def createId(implicit namespace: String): Version[String, Int] =
    Version(randomAlphanumeric, 0)
}
