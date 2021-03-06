package weco.storage.store.memory

import org.apache.commons.io.IOUtils
import weco.storage.store._
import weco.storage.streaming.Codec
import weco.storage.streaming.Codec

class MemoryTypedStore[Ident, T](initialEntries: Map[Ident, T] =
                                   Map.empty[Ident, T])(
  implicit val streamStore: MemoryStreamStore[Ident],
  val codec: Codec[T]
) extends TypedStore[Ident, T] {

  val initial = initialEntries.map {
    case (location, bytes) =>
      location -> IOUtils.toByteArray(
        codec.toStream(bytes).right.get
      )
  }

  streamStore.memoryStore.entries =
    streamStore.memoryStore.entries ++ initial
}

object MemoryTypedStore {
  def apply[Ident, T](initialEntries: Map[Ident, T] = Map.empty[Ident, T])(
    implicit codec: Codec[T],
    streamStore: MemoryStreamStore[Ident] = MemoryStreamStore[Ident]()
  ): MemoryTypedStore[Ident, T] = new MemoryTypedStore(initialEntries)
}
