package uk.ac.wellcome.storage.store.azure

import com.azure.storage.blob.BlobServiceClient
import uk.ac.wellcome.storage.store.Writable
import uk.ac.wellcome.storage.streaming.InputStreamWithLength
import uk.ac.wellcome.storage.{Identified, ObjectLocation, StoreWriteError}

import scala.util.{Failure, Success, Try}

trait AzureStreamWritable extends Writable[ObjectLocation, InputStreamWithLength] {
  implicit val blobClient: BlobServiceClient

  override def put(location: ObjectLocation)(inputStream: InputStreamWithLength): WriteEither =
    Try {
      val individualBlobClient =
        blobClient
          .getBlobContainerClient(location.namespace)
          .getBlobClient(location.path)

      individualBlobClient.upload(
        inputStream, inputStream.length, true
      )
    } match {
      case Success(_)         => Right(Identified(location, inputStream))
      case Failure(throwable) => Left(StoreWriteError(throwable))
    }
}
