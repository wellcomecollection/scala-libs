package uk.ac.wellcome.storage.azure

import com.azure.storage.blob.models.BlobStorageException
import uk.ac.wellcome.storage.{
  DoesNotExistError,
  ReadError,
  RetryableError,
  StoreReadError
}

object AzureStorageErrors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: BlobStorageException if exc.getStatusCode == 404 =>
      DoesNotExistError(exc)
    case exc if exc.getMessage.contains("TimeoutException") =>
      // Timeout errors from Azure should be retried and are in the form
      // "reactor.core.Exceptions$ReactiveException: java.util.concurrent.TimeoutException: Did not
      // observe any item or terminal signal within 60000ms in 'map' (and no fallback has been configured)"
      new StoreReadError(exc) with RetryableError
    case exc =>
      StoreReadError(exc)
  }
}
