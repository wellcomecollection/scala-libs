package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.models.{BlobErrorCode, BlobStorageException}
import uk.ac.wellcome.storage.store.Writable
import uk.ac.wellcome.storage.streaming.InputStreamWithLength
import uk.ac.wellcome.storage.{
  Identified,
  ObjectLocation,
  OverwriteError,
  StoreWriteError
}

import scala.util.{Failure, Success, Try}

trait AzureStreamWritable
    extends Writable[ObjectLocation, InputStreamWithLength] {
  implicit val blobClient: BlobServiceClient
  val allowOverwrites: Boolean

  override def put(location: ObjectLocation)(
    inputStream: InputStreamWithLength): WriteEither =
    Try {
      val individualBlobClient =
        blobClient
          .getBlobContainerClient(location.namespace)
          .getBlobClient(location.path)

      individualBlobClient.upload(
        inputStream,
        inputStream.length.toLong,
        allowOverwrites
      )

    } match {
      case Success(_) => Right(Identified(location, inputStream))
      case Failure(
          exc: BlobStorageException
          ) if exc.getErrorCode == BlobErrorCode.BLOB_ALREADY_EXISTS =>
        Left(OverwriteError(exc))
      case Failure(throwable) => Left(StoreWriteError(throwable))
    }
}
