package weco.storage.locking

trait Lock[Ident, ContextId] {
  val id: Ident
  val contextId: ContextId
}
