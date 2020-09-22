package uk.ac.wellcome.storage.services

import uk.ac.wellcome.storage.ReadError
import uk.ac.wellcome.storage.models.ByteRange

trait RangedReader[Ident] {
  def getBytes(id: Ident, range: ByteRange): Either[ReadError, Array[Byte]]
}
