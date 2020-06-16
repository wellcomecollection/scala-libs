package uk.ac.wellcome.storage.store.s3

import java.net.SocketTimeoutException

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.streaming._

trait S3StreamReadable extends RetryableReadable[InputStreamWithLength] {

  implicit val s3Client: AmazonS3
  val maxRetries: Int

  def retryableGetFunction(location: ObjectLocation): InputStreamWithLength = {
    val retrievedObject = s3Client.getObject(location.namespace, location.path)

    new InputStreamWithLength(
      retrievedObject.getObjectContent,
      length = retrievedObject.getObjectMetadata.getContentLength
    )
  }

  def buildGetError(throwable: Throwable): ReadError =
    throwable match {
      case exc: AmazonS3Exception
          if exc.getMessage.startsWith("The specified key does not exist") ||
            exc.getMessage.startsWith("The specified bucket does not exist") =>
        DoesNotExistError(exc)

      case exc: AmazonS3Exception
          if exc.getMessage.startsWith("The specified bucket is not valid") =>
        StoreReadError(exc)

      case exc: AmazonS3Exception
          if exc.getMessage.startsWith(
            "We encountered an internal error. Please try again.") ||
            exc.getMessage.startsWith("Please reduce your request rate.") =>
        new StoreReadError(exc) with RetryableError

      case exc: SocketTimeoutException =>
        new StoreReadError(exc) with RetryableError

      case _ =>
        warn(s"Unrecognised error inside S3StreamStore.get: $throwable")
        StoreReadError(throwable)
    }
}
