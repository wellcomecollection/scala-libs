package uk.ac.wellcome.storage.azure

import com.azure.storage.blob.models.BlobStorageException
import uk.ac.wellcome.storage.{DoesNotExistError, ReadError, StoreReadError}

object AzureStorageErrors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: BlobStorageException if exc.getStatusCode == 404 =>
      DoesNotExistError(exc)
    case exc =>
      StoreReadError(exc)
  }
}
