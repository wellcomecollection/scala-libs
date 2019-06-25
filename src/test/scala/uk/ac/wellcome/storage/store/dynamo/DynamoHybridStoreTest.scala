package uk.ac.wellcome.storage.store.dynamo

import org.scanamo.auto._
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.fixtures.{DynamoFixtures, S3Fixtures}
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.generators.{MetadataGenerators, Record, RecordGenerators}
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.store.{HybridIndexedStoreEntry, HybridStoreEntry, HybridStoreTestCases, TypedStoreEntry}
import uk.ac.wellcome.storage.store.s3.{S3StreamStore, S3TypedStore}

class DynamoHybridStoreTest extends HybridStoreTestCases[
  Version[String, Int],
  ObjectLocation,
  Record,
  Map[String, String],
  Unit,
  S3TypedStore[Record],
  DynamoHashStore[String, Int, HybridIndexedStoreEntry[Version[String, Int], ObjectLocation, Map[String, String]]],
  (Bucket, Table)
] with RecordGenerators with S3Fixtures with DynamoFixtures with MetadataGenerators {
  type S3TypedStoreImpl = S3TypedStore[Record]
  type DynamoIndexedStoreImpl = DynamoHashStore[String, Int, HybridIndexedStoreEntry[Version[String, Int], ObjectLocation, Map[String, String]]]

  override def createTable(table: Table): Table =
    createTableWithHashKey(table, keyName = "hashKey")

  def createPrefix(implicit context: (Bucket, Table)): ObjectLocationPrefix = {
    val (bucket, _) = context
    ObjectLocationPrefix(
      namespace = bucket.name,
      path = randomAlphanumeric
    )
  }

  override def withHybridStoreImpl[R](
    typedStore: S3TypedStoreImpl,
    indexedStore: DynamoIndexedStoreImpl)(testWith: TestWith[HybridStoreImpl, R])(implicit context: (Bucket, Table)): R = {
    implicit val underlyingTypedStore: S3TypedStoreImpl = typedStore
    implicit val underlyingIndexedStore: DynamoIndexedStoreImpl = indexedStore

    val hybridStore = new DynamoHybridStore[Record, Map[String, String]](createPrefix)

    testWith(hybridStore)
  }

  override def withTypedStoreImpl[R](testWith: TestWith[S3TypedStoreImpl, R])(implicit context: (Bucket, Table)): R =
    testWith(S3TypedStore[Record])

  override def withIndexedStoreImpl[R](testWith: TestWith[DynamoIndexedStoreImpl, R])(implicit context: (Bucket, Table)): R = {
    val (_, table) = context

    testWith(
      new DynamoIndexedStoreImpl(
        config = createDynamoConfigWith(table)
      )
    )
  }

  override def createTypedStoreId(implicit bucket: Unit): ObjectLocation =
    createObjectLocation

  override def createMetadata: Map[String, String] = createValidMetadata

  override def withBrokenPutTypedStoreImpl[R](testWith: TestWith[S3TypedStoreImpl, R])(implicit context: (Bucket, Table)): R = {
    val s3StreamStore: S3StreamStore = new S3StreamStore()

    testWith(
      new S3TypedStore[Record]()(codec, s3StreamStore) {
        override def put(id: ObjectLocation)(entry: TypedStoreEntry[Record]): WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenGetTypedStoreImpl[R](testWith: TestWith[S3TypedStoreImpl, R])(implicit context: (Bucket, Table)): R = {
    val s3StreamStore: S3StreamStore = new S3StreamStore()

    testWith(
      new S3TypedStore[Record]()(codec, s3StreamStore) {
        override def get(id: ObjectLocation): ReadEither =
          Left(StoreReadError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenPutIndexedStoreImpl[R](testWith: TestWith[DynamoIndexedStoreImpl, R])(implicit context: (Bucket, Table)): R = {
    val (_, table) = context

    testWith(
      new DynamoIndexedStoreImpl(config = createDynamoConfigWith(table)) {
        override def put(id: Version[String, Int])(t: HybridIndexedStoreEntry[Version[String, Int], ObjectLocation, Map[String, String]]): WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenGetIndexedStoreImpl[R](testWith: TestWith[DynamoIndexedStoreImpl, R])(implicit context: (Bucket, Table)): R = {
    val (_, table) = context

    testWith(
      new DynamoIndexedStoreImpl(config = createDynamoConfigWith(table)) {
        override def get(id: Version[String, Int]): ReadEither =
          Left(StoreReadError(new Error("BOOM!")))
      }
    )
  }

  override def withStoreContext[R](testWith: TestWith[(Bucket, Table), R]): R =
    withLocalS3Bucket { bucket =>
      withLocalDynamoDbTable { table =>
        testWith((bucket, table))
      }
    }

  override def createT: HybridStoreEntry[Record, Map[String, String]] =
    HybridStoreEntry(createRecord, createValidMetadata)

  override def withNamespace[R](testWith: TestWith[Unit, R]): R =
    testWith(())

  override def createId(implicit namespace: Unit): Version[String, Int] =
    Version(id = randomAlphanumeric, version = 1)
}
