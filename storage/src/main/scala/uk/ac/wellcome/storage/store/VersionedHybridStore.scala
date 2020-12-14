package uk.ac.wellcome.storage.store

class VersionedHybridStore[Id, V, TypedStoreId, T](
  val hybridStore: HybridStoreWithMaxima[Id, V, TypedStoreId, T]
)(implicit N: Numeric[V])
    extends VersionedStore[Id, V, T](hybridStore)
