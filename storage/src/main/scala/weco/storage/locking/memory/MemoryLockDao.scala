package weco.storage.locking.memory

import grizzled.slf4j.Logging
import weco.storage.locking.{Lock, LockDao, LockFailure}

class MemoryLockDao[MemoryIdent, MemoryContextId]
    extends LockDao[MemoryIdent, MemoryContextId]
    with Logging {
  type MemoryLock = PermanentLock[MemoryIdent, MemoryContextId]

  var locks: Map[MemoryIdent, MemoryLock] = Map.empty

  override def lock(id: MemoryIdent, contextId: MemoryContextId): LockResult =
    synchronized {
      info(s"Locking ID <$id> in context <$contextId>")

      locks.get(id) match {
        case Some(r @ PermanentLock(_, existingContextId))
            if contextId == existingContextId =>
          Right(r)
        case Some(PermanentLock(_, existingContextId))
            if contextId != existingContextId =>
          Left(
            LockFailure(
              id,
              new Throwable(
                s"Failed to lock <$id> in context <$contextId>; already locked as <$existingContextId>")
            )
          )
        case _ =>
          val rowLock = PermanentLock(
            id = id,
            contextId = contextId
          )
          locks = locks ++ Map(id -> rowLock)

          Right(rowLock)
      }
    }

  override def unlock(contextId: MemoryContextId): UnlockResult =
    synchronized {
      info(s"Unlocking for context <$contextId>")

      locks = locks.filter {
        case (id, PermanentLock(_, lockContextId)) =>
          debug(s"Inspecting $id")
          contextId != lockContextId
      }

      Right(())
    }

  def getCurrentLocks: Set[MemoryIdent] =
    locks.keys.toSet
}

case class PermanentLock[Ident, ContextId](id: Ident, contextId: ContextId)
    extends Lock[Ident, ContextId]
