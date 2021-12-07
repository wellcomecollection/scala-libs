package weco.storage.store.memory

import weco.fixtures.TestWith
import weco.storage.{MaximaReadError, StoreReadError, StoreWriteError, Version}
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.maxima.memory.MemoryMaxima
import weco.storage.store.VersionedStoreWithOverwriteTestCases

class MemoryVersionedHybridStoreTest
    extends VersionedStoreWithOverwriteTestCases[
      String,
      Record,
      MemoryHybridStoreWithMaxima[String, Record]]
    with RecordGenerators {

  override def withFailingGetVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    implicit val indexedStore
      : MemoryStore[Version[String, Int], String] with MemoryMaxima[String,
                                                                    String] =
      new MemoryStore[Version[String, Int], String](Map.empty)
      with MemoryMaxima[String, String]

    implicit val typedStore: MemoryTypedStore[String, Record] =
      MemoryTypedStore[String, Record](initialEntries = Map.empty)

    val store = new MemoryHybridStoreWithMaxima[String, Record]() {
      override def max(id: String): MaxEither =
        Left(MaximaReadError(new Error("BOOM!")))
    }

    initialEntries.map {
      case (k, v) => store.put(k)(v).value
    }

    val versionedHybridStore =
      new MemoryVersionedHybridStore[String, Record](store) {
        override def get(id: Version[String, Int]): ReadEither = {
          Left(StoreReadError(new Error("BOOM!")))
        }
      }

    testWith(versionedHybridStore)
  }

  override def withFailingPutVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    withVersionedStoreContext { storeContext =>
      initialEntries.map {
        case (k, v) => storeContext.put(k)(v).value
      }

      val versionedHybridStore =
        new MemoryVersionedHybridStore[String, Record](storeContext) {
          override def put(id: Version[String, Int])(t: Record): WriteEither =
            Left(StoreWriteError(new Error("BOOM!")))
        }

      testWith(versionedHybridStore)
    }
  }

  override def createIdent: String = randomAlphanumeric()

  override def withVersionedStoreImpl[R](
    initialEntries: Entries,
    storeContext: MemoryHybridStoreWithMaxima[String, Record])(
    testWith: TestWith[VersionedStoreImpl, R]): R = {

    initialEntries.map {
      case (k, v) => storeContext.put(k)(v).value
    }

    val versionedHybridStore =
      new MemoryVersionedHybridStore[String, Record](storeContext)

    testWith(versionedHybridStore)
  }

  override def withVersionedStoreContext[R](
    testWith: TestWith[MemoryHybridStoreWithMaxima[String, Record], R]): R = {
    implicit val indexedStore
      : MemoryStore[Version[String, Int], String] with MemoryMaxima[String,
                                                                    String] =
      new MemoryStore[Version[String, Int], String](Map.empty)
      with MemoryMaxima[String, String]

    implicit val typedStore: MemoryTypedStore[String, Record] =
      MemoryTypedStore[String, Record](initialEntries = Map.empty)

    testWith(new MemoryHybridStoreWithMaxima[String, Record]())
  }

  override def createT: Record = createRecord

  override def withStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record],
    storeContext: MemoryHybridStoreWithMaxima[String, Record])(
    testWith: TestWith[StoreImpl, R]): R =
    withVersionedStoreImpl(initialEntries, storeContext)(testWith)

  override def withStoreContext[R](
    testWith: TestWith[MemoryHybridStoreWithMaxima[String, Record], R]): R =
    withVersionedStoreContext(testWith)

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createId(implicit namespace: String): Version[String, Int] =
    Version(randomAlphanumeric(), 0)
}
