package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.specialized.BlobInputStream
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

trait AzureStreamReadable extends RetryableReadable[InputStreamWithLength] {
  implicit val blobClient: BlobServiceClient
  val maxRetries: Int

  def retryableGetFunction(location: ObjectLocation): InputStreamWithLength = {
    val blobInputStream = blobClient
      .getBlobContainerClient(location.namespace)
      .getBlobClient(location.path)
      .openInputStream()

    buildInputStream(blobInputStream)
  }

  private def buildInputStream(
    blobInputStream: BlobInputStream): InputStreamWithLength = {
    new InputStreamWithLength(
      blobInputStream,
      length = blobInputStream.getProperties.getBlobSize,
    )
  }

  def buildGetError(throwable: Throwable): ReadError =
    throwable match {
      case exc: BlobStorageException if exc.getStatusCode == 404 =>
        DoesNotExistError(exc)
      case _ =>
        warn(s"Unrecognised error inside AzureReadable.get: $throwable")
        StoreReadError(throwable)
    }
}
