package weco.storage.store.memory

import weco.storage.store.VersionedStore

class MemoryVersionedHybridStore[Id, T](
  store: MemoryHybridStoreWithMaxima[Id, T]
) extends VersionedStore[Id, Int, T](store)
