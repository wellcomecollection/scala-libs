package weco.storage.store.memory

import weco.Logging
import weco.storage.store.Store
import weco.storage.{DoesNotExistError, Identified, ReadError, WriteError}

class MemoryStore[Ident, T](initialEntries: Map[Ident, T])
    extends Store[Ident, T]
    with Logging
    with MemoryStoreBase[Ident, T] {

  var entries: Map[Ident, T] = initialEntries

  override def put(id: Ident)(t: T): Either[WriteError, Identified[Ident, T]] =
    synchronized {
      debug(s"put($id)($t)")

      debug(s"Pre-update state: $entries")

      entries = entries.updated(id, t)

      debug(s"Post-update state: $entries")

      Right(Identified(id, t))
    }

  override def get(id: Ident): Either[ReadError, Identified[Ident, T]] = {
    debug(s"get($id)")

    val result = entries.get(id) match {
      case Some(t) => Right(Identified(id, t))
      case None =>
        Left(
          DoesNotExistError(
            new Throwable(s"There is no entry for id=$id")
          ))
    }

    debug(s"Got $result")

    result
  }
}
