package uk.ac.wellcome.storage.store.s3

import com.amazonaws.services.s3.AmazonS3
import uk.ac.wellcome.storage._
import uk.ac.wellcome.storage.s3.{S3Errors, S3ObjectLocation}
import uk.ac.wellcome.storage.store.RetryableReadable
import uk.ac.wellcome.storage.streaming._

trait S3StreamReadable
    extends RetryableReadable[S3ObjectLocation, InputStreamWithLength] {
  implicit val s3Client: AmazonS3
  val maxRetries: Int

  def retryableGetFunction(
    location: S3ObjectLocation): InputStreamWithLength = {
    val retrievedObject = s3Client.getObject(location.bucket, location.key)

    new InputStreamWithLength(
      retrievedObject.getObjectContent,
      length = retrievedObject.getObjectMetadata.getContentLength
    )
  }

  def buildGetError(throwable: Throwable): ReadError =
    S3Errors.readErrors(throwable)
}
