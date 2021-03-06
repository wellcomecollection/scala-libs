package weco.storage.store.memory

import weco.fixtures.TestWith
import weco.storage.store.fixtures.StreamStoreFixtures
import weco.storage.streaming.Codec.bytesCodec
import weco.storage.streaming.InputStreamWithLength
import weco.storage.store.fixtures.StreamStoreFixtures
import weco.storage.streaming.InputStreamWithLength

trait MemoryStreamStoreFixtures[Ident]
    extends StreamStoreFixtures[
      Ident,
      MemoryStreamStore[Ident],
      MemoryStore[Ident, Array[Byte]]] {
  def withMemoryStreamStoreImpl[R](
    underlying: MemoryStore[Ident, Array[Byte]],
    initialEntries: Map[Ident, InputStreamWithLength])(
    testWith: TestWith[MemoryStreamStore[Ident], R]): R = {
    val memoryStoreEntries =
      initialEntries.map {
        case (id, inputStream) =>
          (
            id,
            bytesCodec.fromStream(inputStream).value
          )
      }

    underlying.entries = underlying.entries ++ memoryStoreEntries

    testWith(
      new MemoryStreamStore[Ident](underlying)
    )
  }

  override def withStreamStoreImpl[R](
    storeContext: MemoryStore[Ident, Array[Byte]],
    initialEntries: Map[Ident, InputStreamWithLength])(
    testWith: TestWith[MemoryStreamStore[Ident], R]): R =
    withMemoryStreamStoreImpl(storeContext, initialEntries) { streamStore =>
      testWith(streamStore)
    }

  override def withStreamStoreContext[R](
    testWith: TestWith[MemoryStore[Ident, Array[Byte]], R]): R =
    testWith(
      new MemoryStore[Ident, Array[Byte]](initialEntries = Map.empty)
    )
}
