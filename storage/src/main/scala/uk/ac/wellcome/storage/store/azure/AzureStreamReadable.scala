package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.azure.AzureStorageErrors
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

trait AzureStreamReadable extends RetryableReadable[ObjectLocation, InputStreamWithLength] {
  implicit val blobClient: BlobServiceClient
  val maxRetries: Int

  def retryableGetFunction(location: ObjectLocation): InputStreamWithLength = {
    val blobInputStream = blobClient
      .getBlobContainerClient(location.namespace)
      .getBlobClient(location.path)
      .openInputStream()

    new InputStreamWithLength(
      inputStream = blobInputStream,
      length = blobInputStream.getProperties.getBlobSize,
    )
  }

  def buildGetError(throwable: Throwable): ReadError =
    AzureStorageErrors.readErrors(throwable)
}
