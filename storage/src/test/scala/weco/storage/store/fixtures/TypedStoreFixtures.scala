package weco.storage.store.fixtures

import weco.fixtures.TestWith
import weco.storage.store.{StreamStore, TypedStore}
import weco.storage.streaming.Codec

trait TypedStoreFixtures[Ident,
                         T,
                         StreamStoreImpl <: StreamStore[Ident],
                         TypedStoreImpl <: TypedStore[Ident, T],
                         StreamStoreContext]
    extends StreamStoreFixtures[Ident, StreamStoreImpl, StreamStoreContext] {
  implicit val codec: Codec[T]

  def withTypedStore[R](streamStore: StreamStoreImpl,
                        initialEntries: Map[Ident, T])(
    testWith: TestWith[TypedStoreImpl, R])(implicit codec: Codec[T]): R

  def withTypedStoreImpl[R](storeContext: StreamStoreContext,
                            initialEntries: Map[Ident, T])(
    testWith: TestWith[TypedStoreImpl, R])(implicit codec: Codec[T]): R =
    withStreamStoreImpl(storeContext, initialEntries = Map.empty) {
      streamStore =>
        withTypedStore(streamStore, initialEntries) { typedStore =>
          testWith(typedStore)
        }
    }
}
