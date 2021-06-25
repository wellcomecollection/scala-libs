package weco.storage.store.fixtures

import org.scalatest.EitherValues
import weco.fixtures.TestWith
import weco.storage.store.StreamStore
import weco.storage.streaming.InputStreamWithLength

trait StreamStoreFixtures[
  Ident,
  StreamStoreImpl <: StreamStore[Ident],
  StreamStoreContext]
    extends EitherValues {
  def withStreamStoreImpl[R](
    context: StreamStoreContext,
    initialEntries: Map[Ident, InputStreamWithLength])(
    testWith: TestWith[StreamStoreImpl, R]): R

  def withStreamStoreContext[R](testWith: TestWith[StreamStoreContext, R]): R
}
