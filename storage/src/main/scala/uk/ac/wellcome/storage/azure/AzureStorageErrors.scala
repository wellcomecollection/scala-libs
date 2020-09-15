package uk.ac.wellcome.storage.azure

import com.azure.storage.blob.models.BlobStorageException
import uk.ac.wellcome.storage.{DoesNotExistError, ReadError, RetryableError, StoreReadError}

object AzureStorageErrors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: BlobStorageException if exc.getStatusCode == 404 =>
      DoesNotExistError(exc)
    case exc if exc.getMessage.contains("TimeoutException") =>
      new StoreReadError(exc) with RetryableError
    case exc =>
      StoreReadError(exc)
  }
}
