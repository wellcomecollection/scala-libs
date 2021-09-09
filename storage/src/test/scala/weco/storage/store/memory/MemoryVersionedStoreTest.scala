package weco.storage.store.memory

import weco.fixtures.TestWith
import weco.storage._
import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.maxima.memory.MemoryMaxima
import weco.storage.store.VersionedStoreWithOverwriteTestCases

class MemoryVersionedStoreTest
    extends VersionedStoreWithOverwriteTestCases[
      String,
      Record,
      MemoryStore[Version[String, Int], Record] with MemoryMaxima[String,
                                                                  Record]]
    with RecordGenerators {

  type UnderlyingMemoryStore =
    MemoryStore[Version[String, Int], Record] with MemoryMaxima[String, Record]

  override def createIdent: String = randomAlphanumeric()
  override def createT: Record = createRecord

  override def withVersionedStoreImpl[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    val store = new MemoryStore[Version[String, Int], Record](initialEntries)
    with MemoryMaxima[String, Record]
    testWith(new MemoryVersionedStore(store))
  }

  override def withVersionedStoreImpl[R](
    initialEntries: Entries,
    storeContext: MemoryStore[Version[String, Int], Record] with MemoryMaxima[
      String,
      Record])(testWith: TestWith[VersionedStoreImpl, R]): R = {
    initialEntries.map {
      case (k, v) => storeContext.put(k)(v)
    }

    testWith(new MemoryVersionedStore(storeContext))
  }

  override def withVersionedStoreContext[R](
    testWith: TestWith[UnderlyingMemoryStore, R]): R =
    testWith(
      new MemoryStore[Version[String, Int], Record](Map.empty)
      with MemoryMaxima[String, Record])

  override def withFailingGetVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    val store = new MemoryStore[Version[String, Int], Record](initialEntries)
    with MemoryMaxima[String, Record] {
      override def get(id: Version[String, Int]): ReadEither =
        Left(StoreReadError(new Error("BOOM!")))

      override def max(id: String): MaxEither =
        Left(MaximaReadError(new Error("BOOM!")))
    }
    testWith(new MemoryVersionedStore(store))
  }

  override def withFailingPutVersionedStore[R](initialEntries: Entries)(
    testWith: TestWith[VersionedStoreImpl, R]): R = {
    val store = new MemoryStore[Version[String, Int], Record](initialEntries)
    with MemoryMaxima[String, Record] {
      override def put(id: Version[String, Int])(t: Record)
        : Either[WriteError, Identified[Version[String, Int], Record]] = {
        Left(StoreWriteError(new Error("BOOM!")))
      }
    }
    testWith(new MemoryVersionedStore(store))
  }

  override def withStoreContext[R](
    testWith: TestWith[
      MemoryStore[Version[String, Int], Record] with MemoryMaxima[String,
                                                                  Record],
      R]): R =
    withVersionedStoreContext(testWith)

  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createId(implicit namespace: String): Version[String, Int] =
    Version(randomAlphanumeric(), 0)

  override def withStoreImpl[R](
    initialEntries: Map[Version[String, Int], Record],
    storeContext: MemoryStore[Version[String, Int], Record] with MemoryMaxima[
      String,
      Record])(testWith: TestWith[StoreImpl, R]): R =
    withVersionedStoreImpl(initialEntries, storeContext)(testWith)

  it("allows writing the same value twice to a given id/version, even if a higher version exists") {
    val id = createIdent
    val t = createT

    withVersionedStoreImpl(initialEntries = Map()) { store =>
      store.put(Version(id, 0))(t) shouldBe Right(Identified(Version(id, 0), t))
      store.put(Version(id, 1))(t) shouldBe Right(Identified(Version(id, 1), t))
      store.put(Version(id, 0))(t) shouldBe Right(Identified(Version(id, 0), t))
      store.put(Version(id, 0))(t) shouldBe Right(Identified(Version(id, 0), t))
    }
  }
}
