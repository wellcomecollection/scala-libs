package weco.storage.store.memory

import weco.storage.Version
import weco.storage.maxima.Maxima
import weco.storage.maxima.memory.MemoryMaxima
import weco.storage.store.VersionedStore
import weco.storage.maxima.Maxima

class MemoryVersionedStore[Id, T](
  store: MemoryStore[Version[Id, Int], T] with Maxima[Id, Version[Id, Int], T]
) extends VersionedStore[Id, Int, T](store)

object MemoryVersionedStore {
  def apply[Id, T](
    initialEntries: Map[Version[Id, Int], T]): MemoryVersionedStore[Id, T] =
    new MemoryVersionedStore[Id, T](
      store = new MemoryStore[Version[Id, Int], T](initialEntries)
      with MemoryMaxima[Id, T]
    )
}
