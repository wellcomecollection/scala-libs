package weco.storage.store.memory

import weco.storage.store._
import weco.storage.streaming.Codec
import weco.storage.streaming.Codec

class MemoryHybridStore[Ident, T](
  implicit val typedStore: MemoryTypedStore[String, T],
  val indexedStore: MemoryStore[Ident, String],
  val codec: Codec[T]
) extends HybridStore[Ident, String, T] {

  override protected def createTypeStoreId(id: Ident): String = id.toString
}
