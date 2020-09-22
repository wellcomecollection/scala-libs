package uk.ac.wellcome.storage.services

import uk.ac.wellcome.storage.StorageError

trait ObjectExists[Ident] {
  def exists(id: Ident): Either[StorageError, Boolean]
}
