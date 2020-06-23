package uk.ac.wellcome.storage.tags.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.azure.AzureStorageErrors
import uk.ac.wellcome.storage.{
  ObjectLocation,
  ReadError,
  StoreWriteError,
  WriteError
}
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.tags.Tags

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class AzureBlobMetadata(val maxRetries: Int = 2)(
  implicit blobClient: BlobServiceClient)
    extends Tags[ObjectLocation]
    with RetryableReadable[Map[String, String]] {

  override protected def writeTags(
    id: ObjectLocation,
    tags: Map[String, String]): Either[WriteError, Map[String, String]] = {
    Try {
      val individualBlobClient =
        blobClient
          .getBlobContainerClient(id.namespace)
          .getBlobClient(id.path)

      individualBlobClient.setMetadata(tags.asJava)
    } match {
      case Success(_)   => Right(tags)
      case Failure(err) => Left(StoreWriteError(err))
    }
  }

  override def retryableGetFunction(
    location: ObjectLocation): Map[String, String] = {
    val individualBlobClient =
      blobClient
        .getBlobContainerClient(location.namespace)
        .getBlobClient(location.path)

    individualBlobClient.getProperties.getMetadata.asScala.toMap
  }

  override def buildGetError(throwable: Throwable): ReadError =
    AzureStorageErrors.readErrors(throwable)
}
