package uk.ac.wellcome.storage.store.memory

import uk.ac.wellcome.storage.Version
import uk.ac.wellcome.storage.maxima.Maxima
import uk.ac.wellcome.storage.store.{HybridStoreWithMaxima, Store}
import uk.ac.wellcome.storage.streaming.Codec

class MemoryHybridStoreWithMaxima[Id, T](
  implicit val typedStore: MemoryTypedStore[String, T],
  val indexedStore: Store[Version[Id, Int], String] with Maxima[Id, Version[Id, Int], String],
  val codec: Codec[T]
) extends HybridStoreWithMaxima[Id, Int, String, T] {

  override protected def createTypeStoreId(id: Version[Id, Int]): String =
    id.toString
}
