package weco.storage.store.fixtures

import weco.fixtures.TestWith
import weco.storage._
import weco.storage.store.VersionedStore

trait VersionedStoreFixtures[Id, V, T, VersionedStoreContext] {
  type VersionedStoreImpl = VersionedStore[Id, V, T]

  type Entries = Map[Version[Id, V], T]

  def createIdent: Id
  def createT: T

  def withVersionedStoreImpl[R](initialEntries: Entries,
                                storeContext: VersionedStoreContext)(
    testWith: TestWith[VersionedStoreImpl, R]): R
  def withVersionedStoreContext[R](
    testWith: TestWith[VersionedStoreContext, R]): R

  def withVersionedStoreImpl[R](initialEntries: Entries = Map.empty)(
    testWith: TestWith[VersionedStoreImpl, R]): R =
    withVersionedStoreContext { storeContext =>
      withVersionedStoreImpl(initialEntries, storeContext) {
        versionedStoreImpl =>
          testWith(versionedStoreImpl)
      }
    }
}
