package uk.ac.wellcome.storage.store.memory

import java.util.UUID

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.generators.{
  Record,
  RecordGenerators
}
import uk.ac.wellcome.storage.store._

class MemoryHybridStoreTest
    extends HybridStoreWithOverwritesTestCases[
      UUID,
      String,
      Record,
      String,
      MemoryTypedStore[String, Record],
      MemoryStore[UUID, String],
      (MemoryTypedStore[String, Record], MemoryStore[UUID, String])]
    with RecordGenerators
    with MemoryTypedStoreFixtures[String, Record] {

  type MemoryIndexedStoreImpl = MemoryStore[UUID, String]
  type MemoryTypedStoreImpl = MemoryTypedStore[String, Record]

  type Context = (MemoryTypedStoreImpl, MemoryIndexedStoreImpl)

  override def withHybridStoreImpl[R](typedStore: MemoryTypedStoreImpl,
                                      indexedStore: MemoryIndexedStoreImpl)(
    testWith: TestWith[HybridStoreImpl, R])(implicit context: Context): R =
    testWith(
      new MemoryHybridStore[UUID, Record]()(
        typedStore,
        indexedStore,
        codec)
    )

  override def withTypedStoreImpl[R](
    testWith: TestWith[MemoryTypedStoreImpl, R])(
    implicit context: Context): R = {
    val (typedStore, _) = context

    testWith(typedStore)
  }

  override def withIndexedStoreImpl[R](
    testWith: TestWith[MemoryIndexedStoreImpl, R])(
    implicit context: Context): R = {
    val (_, indexedStore) = context

    testWith(indexedStore)
  }

  override def createTypedStoreId(implicit namespace: String): String =
    s"$namespace/${randomAlphanumeric()}"

  override def withStoreContext[R](testWith: TestWith[Context, R]): R = {
    implicit val underlyingStreamStore: MemoryStreamStore[String] =
      MemoryStreamStore[String]()

    withMemoryTypedStoreImpl(initialEntries = Map.empty) { typedStore =>
      val indexedStore = new MemoryIndexedStoreImpl(initialEntries = Map.empty)

      testWith((typedStore, indexedStore))
    }
  }

  override def createT: Record = createRecord
    
  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createId(implicit namespace: String): UUID = randomUUID

  override def withBrokenPutTypedStoreImpl[R](
    testWith: TestWith[MemoryTypedStoreImpl, R])(
    implicit context: Context): R = {
    implicit val underlyingStreamStore: MemoryStreamStore[String] =
      MemoryStreamStore[String]()

    testWith(
      new MemoryTypedStoreImpl(initialEntries = Map.empty) {
        override def put(id: String)(entry: Record): WriteEither =
          Left(StoreWriteError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenGetTypedStoreImpl[R](
    testWith: TestWith[MemoryTypedStoreImpl, R])(
    implicit context: Context): R = {
    implicit val underlyingStreamStore: MemoryStreamStore[String] =
      MemoryStreamStore[String]()

    testWith(
      new MemoryTypedStoreImpl(initialEntries = Map.empty) {
        override def get(id: String): ReadEither =
          Left(StoreReadError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenPutIndexedStoreImpl[R](
    testWith: TestWith[MemoryIndexedStoreImpl, R])(
    implicit context: Context): R = {
    testWith(
      new MemoryIndexedStoreImpl(initialEntries = Map.empty) {
        override def put(id: UUID)(t: String): Either[
          WriteError,
          Identified[UUID, String]] =
          Left(StoreWriteError(new Error("BOOM!")))
      }
    )
  }

  override def withBrokenGetIndexedStoreImpl[R](
    testWith: TestWith[MemoryIndexedStoreImpl, R])(
    implicit context: Context): R = {
    testWith(
      new MemoryIndexedStoreImpl(initialEntries = Map.empty) {
        override def get(id: UUID): Either[
          ReadError,
          Identified[UUID, String]] =
          Left(StoreReadError(new Error("BOOM!")))
      }
    )
  }
}
