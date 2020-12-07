package uk.ac.wellcome.storage.store

import uk.ac.wellcome.storage.{Identified, Version}
import uk.ac.wellcome.storage.maxima.Maxima

trait HybridStoreWithMaxima[Id, V, TypedStoreId, T]
    extends HybridStore[Version[Id, V], TypedStoreId, T]
    with Maxima[Id, Version[Id, V], T] {

  override implicit protected val indexedStore: Store[
    Version[Id, V],
    TypedStoreId] with Maxima[Id, Version[Id, V], TypedStoreId]

  override def max(id: Id): MaxEither =
    indexedStore
      .max(id)
      .flatMap {
        case Identified(Version(_, version), typedStoreId) =>
          typedStore
            .get(typedStoreId)
            .map {
              case Identified(_, t) => Identified(Version(id, version), t)
            }
      }
}
