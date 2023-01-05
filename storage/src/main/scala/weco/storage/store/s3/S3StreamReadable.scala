package weco.storage.store.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import weco.storage._
import weco.storage.s3.{S3Errors, S3ObjectLocation}
import weco.storage.store.RetryableReadable
import weco.storage.streaming._

trait S3StreamReadable
    extends RetryableReadable[S3ObjectLocation, InputStreamWithLength] {
  implicit val s3Client: AmazonS3

  override protected def retryableGetFunction(
    location: S3ObjectLocation): InputStreamWithLength = {
    val getRequest = new GetObjectRequest(location.bucket, location.key)

    val retrievedObject = s3Client.getObject(getRequest)

    new InputStreamWithLength(
      retrievedObject.getObjectContent,
      length = retrievedObject.getObjectMetadata.getContentLength
    )
  }

  override protected def buildGetError(throwable: Throwable): ReadError =
    S3Errors.readErrors(throwable)
}
