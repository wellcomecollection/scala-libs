package uk.ac.wellcome.storage.maxima.memory

import uk.ac.wellcome.storage.maxima.Maxima
import uk.ac.wellcome.storage.store.memory.MemoryStoreBase
import uk.ac.wellcome.storage.{
  Identified,
  NoMaximaValueError,
  Version
}

trait MemoryMaxima[Id, T]
    extends Maxima[Id, Version[Id, Int], T]
    with MemoryStoreBase[Version[Id, Int], T] {

  def max(id: Id): MaxEither = {
    val matchingEntries =
      entries
        .filter { case (ident, _) => ident.id == id }

    if (matchingEntries.isEmpty) {
      Left(NoMaximaValueError())
    } else {
      val (maxIdent, maxT) =
        matchingEntries.maxBy { case (ident, _) => ident.version }
      Right(Identified(maxIdent, maxT))
    }
  }
}
