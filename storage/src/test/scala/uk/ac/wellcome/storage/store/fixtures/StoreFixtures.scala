package uk.ac.wellcome.storage.store.fixtures

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.store.Store

trait StoreFixtures[Ident, T, Namespace, StoreContext]
    extends Matchers
    with NamespaceFixtures[Ident, Namespace] {
  type StoreImpl = Store[Ident, T]

  def withStoreImpl[R](
    initialEntries: Map[Ident, T],
    storeContext: StoreContext)(testWith: TestWith[StoreImpl, R]): R

  def withStoreContext[R](testWith: TestWith[StoreContext, R]): R

  def withStoreImpl[R](initialEntries: Map[Ident, T])(
    testWith: TestWith[StoreImpl, R]): R =
    withStoreContext { storeContext =>
      withStoreImpl(initialEntries, storeContext) { storeImpl =>
        testWith(storeImpl)
      }
    }

  def withEmptyStoreImpl[R](testWith: TestWith[StoreImpl, R]): R =
    withStoreImpl(initialEntries = Map.empty) { storeImpl =>
      testWith(storeImpl)
    }

  def createT: T

  def assertEqualT(original: T, stored: T): Assertion =
    original shouldBe stored
}