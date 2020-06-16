package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.specialized.BlobInputStream
import grizzled.slf4j.Logging
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.store.Readable
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait AzureStreamReadable extends Readable[ObjectLocation, InputStreamWithLength] with Logging {
  implicit val blobClient: BlobServiceClient
  val maxRetries: Int

  import RetryOps._

  override def get(location: ObjectLocation): ReadEither = {
    val retryableGet = (getOnce _).retry(maxRetries)

    retryableGet(location)
  }

  private def getOnce(location: ObjectLocation): ReadEither = {
    val openResult =
      Try { blobClient
        .getBlobContainerClient(location.namespace)
        .getBlobClient(location.path)
        .openInputStream()
      }

    openResult match {
      case Success(blobInputStream) =>
        Right(
          Identified(location, buildInputStream(blobInputStream))
        )
      case Failure(err) => Left(buildOpenError(err))
    }
  }

  private def buildInputStream(blobInputStream: BlobInputStream): InputStreamWithLength = {
    // We get a mutable.Map from the S3 SDK, but we want an immutable Map to pass
    // out to the codebase, hence this slightly odd construction!
    val userMetadata = blobInputStream.getProperties.getMetadata
    val metadata = userMetadata
      .keySet()
      .asScala
      .map { k =>
        (k, userMetadata.get(k))
      }
      .toMap

    new InputStreamWithLength(
      blobInputStream,
      length = blobInputStream.getProperties.getBlobSize,
    )
  }

  private def buildOpenError(throwable: Throwable): ReadError =
    throwable match {
      case exc: BlobStorageException if exc.getStatusCode == 404 =>
        DoesNotExistError(exc)
      case _ =>
        warn(s"Unrecognised error inside AzureReadable.get: $throwable")
        StoreReadError(throwable)
    }
}
