package uk.ac.wellcome.storage.store.memory

import uk.ac.wellcome.storage.store.VersionedStore

class MemoryVersionedHybridStore[Id, T](
  store: MemoryHybridStoreWithMaxima[Id, T]
) extends VersionedStore[Id, Int, T](store)
