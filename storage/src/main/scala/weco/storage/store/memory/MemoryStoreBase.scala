package weco.storage.store.memory

trait MemoryStoreBase[Ident, T] {
  var entries: Map[Ident, T]
}
