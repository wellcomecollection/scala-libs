package uk.ac.wellcome.storage.services.memory

import uk.ac.wellcome.storage.services.SizeFinder
import uk.ac.wellcome.storage.store.memory.MemoryStore
import uk.ac.wellcome.storage.{DoesNotExistError, Identified}

class MemorySizeFinder[Ident](
  memoryStore: MemoryStore[Ident, Array[Byte]]
) extends SizeFinder[Ident] {
  override def get(id: Ident): ReadEither =
    memoryStore.entries.get(id) match {
      case Some(entry) => Right(Identified(id, entry.length))
      case None        => Left(DoesNotExistError())
    }
}
