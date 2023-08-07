package weco.storage.store.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import weco.storage._
import weco.storage.providers.s3.{S3Errors, S3ObjectLocation}
import weco.storage.store.RetryableReadable
import weco.storage.streaming._

trait S3StreamReadable
    extends RetryableReadable[S3ObjectLocation, InputStreamWithLength] {
  implicit val s3Client: S3Client

  override protected def retryableGetFunction(
    location: S3ObjectLocation): InputStreamWithLength = {
    val getRequest =
      GetObjectRequest
        .builder()
        .bucket(location.bucket)
        .key(location.key)
        .build()

    val retrievedObject = s3Client.getObject(getRequest)

    new InputStreamWithLength(
      retrievedObject,
      length = retrievedObject.response().contentLength()
    )
  }

  override protected def buildGetError(throwable: Throwable): ReadError =
    S3Errors.readErrors(throwable)
}

class S3StreamReader(val maxRetries: Int = 2)(implicit val s3Client: S3Client)
    extends S3StreamReadable
