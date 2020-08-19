package uk.ac.wellcome.storage.tags.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureStorageErrors}
import uk.ac.wellcome.storage.{ReadError, StoreWriteError, WriteError}
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.tags.Tags

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class AzureBlobMetadata(val maxRetries: Int = 2)(
  implicit blobClient: BlobServiceClient)
    extends Tags[AzureBlobLocation]
    with RetryableReadable[AzureBlobLocation, Map[String, String]] {

  override protected def writeTags(
    id: AzureBlobLocation,
    tags: Map[String, String]): Either[WriteError, Map[String, String]] = {
    Try {
      val individualBlobClient =
        blobClient
          .getBlobContainerClient(id.container)
          .getBlobClient(id.name)

      individualBlobClient.setMetadata(tags.asJava)
    } match {
      case Success(_)   => Right(tags)
      case Failure(err) => Left(StoreWriteError(err))
    }
  }

  override protected def retryableGetFunction(
    location: AzureBlobLocation): Map[String, String] = {
    val individualBlobClient =
      blobClient
        .getBlobContainerClient(location.container)
        .getBlobClient(location.name)

    individualBlobClient.getProperties.getMetadata.asScala.toMap
  }

  override protected def buildGetError(throwable: Throwable): ReadError =
    AzureStorageErrors.readErrors(throwable)
}
