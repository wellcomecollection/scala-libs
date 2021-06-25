package weco.storage.store.memory

import weco.storage.Version
import weco.storage.maxima.Maxima
import weco.storage.store.{HybridStoreWithMaxima, Store}
import weco.storage.streaming.Codec

class MemoryHybridStoreWithMaxima[Id, T](
  implicit val typedStore: MemoryTypedStore[String, T],
  val indexedStore: Store[Version[Id, Int], String] with Maxima[
    Id,
    Version[Id, Int],
    String],
  val codec: Codec[T]
) extends HybridStoreWithMaxima[Id, Int, String, T] {

  override protected def createTypeStoreId(id: Version[Id, Int]): String =
    id.toString
}
