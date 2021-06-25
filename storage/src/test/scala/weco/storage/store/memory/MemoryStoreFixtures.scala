package weco.storage.store.memory

import weco.fixtures.TestWith
import weco.storage.store.fixtures.StoreFixtures

trait MemoryStoreFixtures[Ident, T, Namespace]
    extends StoreFixtures[Ident, T, Namespace, MemoryStore[Ident, T]] {

  override def withStoreImpl[R](initialEntries: Map[Ident, T],
                                storeContext: MemoryStore[Ident, T])(
    testWith: TestWith[StoreImpl, R]): R = {
    storeContext.entries = storeContext.entries ++ initialEntries

    testWith(storeContext)
  }

  def withStoreContext[R](testWith: TestWith[MemoryStore[Ident, T], R]): R =
    testWith(
      new MemoryStore[Ident, T](initialEntries = Map.empty)
    )
}
