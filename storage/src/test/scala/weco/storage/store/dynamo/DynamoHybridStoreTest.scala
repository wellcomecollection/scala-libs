package weco.storage.store.dynamo

import org.scanamo.generic.auto._
import weco.fixtures.TestWith
import weco.storage.{StoreReadError, StoreWriteError, Version}
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.generators.Record
import weco.storage.s3.S3ObjectLocation

import scala.language.higherKinds

class DynamoHybridStoreTest
    extends DynamoHybridStoreTestCases[
      DynamoHashStore[
        String,
        Int,
        S3ObjectLocation]
    ] {
  override def createTable(table: Table): Table =
    createTableWithHashKey(table)

  override def withHybridStoreImpl[R](typedStore: S3TypedStoreImpl,
                                      indexedStore: DynamoIndexedStoreImpl)(
    testWith: TestWith[HybridStoreImpl, R])(
    implicit context: (Bucket, Table)): R = {
    implicit val underlyingTypedStore: S3TypedStoreImpl = typedStore
    implicit val underlyingIndexedStore: DynamoIndexedStoreImpl = indexedStore

    val hybridStore = new DynamoHybridStore[Record](createPrefix)

    testWith(hybridStore)
  }

  override def withIndexedStoreImpl[R](
    testWith: TestWith[DynamoIndexedStoreImpl, R])(
    implicit context: (Bucket, Table)): R = {
    val (_, table) = context

    testWith(
      new DynamoIndexedStoreImpl(
        config = createDynamoConfigWith(table)
      )
    )
  }

  override def withBrokenPutIndexedStoreImpl[R](
    testWith: TestWith[DynamoIndexedStoreImpl, R])(
    implicit context: (Bucket, Table)): R = {
    val (_, table) = context

    testWith(
      new DynamoIndexedStoreImpl(config = createDynamoConfigWith(table)) {
        override def put(id: Version[String, Int])(loc: S3ObjectLocation)
          : WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenGetIndexedStoreImpl[R](
    testWith: TestWith[DynamoIndexedStoreImpl, R])(
    implicit context: (Bucket, Table)): R = {
    val (_, table) = context

    testWith(
      new DynamoIndexedStoreImpl(config = createDynamoConfigWith(table)) {
        override def get(id: Version[String, Int]): ReadEither =
          Left(StoreReadError(new Error("BOOM!")))
      }
    )
  }
}
