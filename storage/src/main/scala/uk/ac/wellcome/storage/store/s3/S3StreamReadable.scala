package uk.ac.wellcome.storage.store.s3

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.s3.S3Errors
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
    S3Errors.readErrors(throwable)
}
