package uk.ac.wellcome.storage.tags.azure

import com.azure.storage.blob.{BlobClient, BlobServiceClient}
import uk.ac.wellcome.storage.azure.{AzureBlobLocation, AzureStorageErrors}
import uk.ac.wellcome.storage.{ObjectLocation, ReadError, StoreWriteError, WriteError}
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.tags.Tags

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class AzureBlobMetadata(val maxRetries: Int = 2)(
  implicit blobClient: BlobServiceClient)
    extends Tags[AzureBlobLocation]
    with RetryableReadable[AzureBlobLocation, Map[String, String]] {

  override protected def writeTags(
    location: AzureBlobLocation,
    tags: Map[String, String]): Either[WriteError, Map[String, String]] = {
    Try {
      val individualBlobClient: BlobClient =
        blobClient
          .getBlobContainerClient(location.container)
          .getBlobClient(location.name)

      individualBlobClient.setMetadata(tags.asJava)
    } match {
      case Success(_)   => Right(tags)
      case Failure(err) => Left(StoreWriteError(err))
    }
  }

  override def retryableGetFunction(
    location: AzureBlobLocation): Map[String, String] = {
    val individualBlobClient =
      blobClient
        .getBlobContainerClient(location.container)
        .getBlobClient(location.name)

    individualBlobClient.getProperties.getMetadata.asScala.toMap
  }

  override def buildGetError(throwable: Throwable): ReadError =
    AzureStorageErrors.readErrors(throwable)
}
