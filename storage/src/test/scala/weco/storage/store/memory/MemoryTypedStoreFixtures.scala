package weco.storage.store.memory

import weco.fixtures.TestWith
import weco.storage.store.fixtures.TypedStoreFixtures
import weco.storage.streaming.Codec

trait MemoryTypedStoreFixtures[Ident, T]
    extends MemoryStreamStoreFixtures[Ident]
    with TypedStoreFixtures[
      Ident,
      T,
      MemoryStreamStore[Ident],
      MemoryTypedStore[Ident, T],
      MemoryStore[Ident, Array[Byte]]] {
  def withTypedStore[R](streamStore: MemoryStreamStore[Ident],
                        initialEntries: Map[Ident, T])(
    testWith: TestWith[MemoryTypedStore[Ident, T], R])(
    implicit codec: Codec[T]): R = {
    implicit val memoryStreamStore: MemoryStreamStore[Ident] = streamStore

    withMemoryTypedStoreImpl(initialEntries) { typedStore =>
      testWith(typedStore)
    }
  }

  def withMemoryTypedStoreImpl[R](initialEntries: Map[Ident, T])(
    testWith: TestWith[MemoryTypedStore[Ident, T], R])(
    implicit streamStore: MemoryStreamStore[Ident],
    codec: Codec[T]): R =
    testWith(
      new MemoryTypedStore[Ident, T](initialEntries)
    )
}
