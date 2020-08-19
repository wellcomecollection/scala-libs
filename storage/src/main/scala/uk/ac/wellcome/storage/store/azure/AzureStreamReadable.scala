package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureStorageErrors}
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

trait AzureStreamReadable
    extends RetryableReadable[AzureBlobLocation, InputStreamWithLength] {
  implicit val blobClient: BlobServiceClient

  override protected def retryableGetFunction(
    location: AzureBlobLocation): InputStreamWithLength = {
    val blobInputStream = blobClient
      .getBlobContainerClient(location.container)
      .getBlobClient(location.name)
      .openInputStream()

    new InputStreamWithLength(
      inputStream = blobInputStream,
      length = blobInputStream.getProperties.getBlobSize,
    )
  }

  override protected def buildGetError(throwable: Throwable): ReadError =
    AzureStorageErrors.readErrors(throwable)
}
