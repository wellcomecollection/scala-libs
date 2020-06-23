package uk.ac.wellcome.storage.store

import uk.ac.wellcome.storage.{ReadError, Version}
import uk.ac.wellcome.storage.maxima.Maxima

trait HybridStoreWithMaxima[Id, V, TypedStoreId, T]
    extends HybridStore[Version[Id, V], TypedStoreId, T]
    with Maxima[Id, V] {

  override implicit protected val indexedStore: Store[
    Version[Id, V],
    TypedStoreId] with Maxima[Id, V]

  override def max(q: Id): Either[ReadError, V] = indexedStore.max(q)
}
