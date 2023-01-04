package weco.storage.services.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, HeadObjectRequest}
import weco.storage._
import weco.storage.s3.{S3Errors, S3ObjectLocation}
import weco.storage.services.SizeFinder
import weco.storage.store.RetryableReadable

import scala.util.{Failure, Success, Try}

class S3SizeFinder(val maxRetries: Int = 3)(implicit s3Client: S3Client)
    extends SizeFinder[S3ObjectLocation]
    with RetryableReadable[S3ObjectLocation, Long] {

  override def retryableGetFunction(location: S3ObjectLocation): Long = {
    val headRequest = new HeadObjectRequest(location.bucket, location.key)

    // We default to using getObjectMetadata, which will return the size
    // immediately on the happy path; we fall back to getObject if it fails
    // because it gives us more more detailed errors from S3 about why
    // a GetObject fails.  This helps us pass more informative messages upstream.
    //
    // e.g. GetObject will return "The bucket name was invalid" rather than
    // "Bad Request".
    //
    Try {
      s3Client
        .headObject(headRequest)
        .contentLength()
    } match {
      case Success(length) => length
      case Failure(_) =>
        val s3Object = s3Client.getObject(
          new GetObjectRequest(location.bucket, location.key)
        )
        val contentLength = s3Object.response().contentLength()

        // TODO: with the V1 SDK, we had to explicitly abort the stream.
        // Do we still need this with the V2 SDK?  Look for a warning in the tests.
        //
        // Abort the stream to avoid getting a warning:
        //
        //    Not all bytes were read from the S3ObjectInputStream, aborting
        //    HTTP connection. This is likely an error and may result in
        //    sub-optimal behavior.
        //
        // s3Object.getObjectContent.abort()
        // s3Object.getObjectContent.close()

        contentLength
    }
  }

  override def buildGetError(throwable: Throwable): ReadError =
    S3Errors.readErrors(throwable)
}
