package weco.storage.services.s3

import java.net.URL
import java.util

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import weco.storage._
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.s3.S3StreamStore
import weco.storage.streaming.Codec.stringCodec
import weco.storage.streaming.InputStreamWithLength

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/** This class allows you to upload a string to an S3 bucket, and get a
  * pre-signed URL for somebody to GET that string out of the bucket.
  *
  * It's based on an example from the AWS SDK for Java docs:
  * https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURLJavaSDK.html
  */
class S3Uploader(implicit val s3Client: AmazonS3) {
  import S3ObjectExists._

  private val s3StreamStore: S3StreamStore = new S3StreamStore()

  // NOTE: checkExists will allow overwriting of existing content if set to false
  // overwriting existing content will change what previously generated URLs return
  def uploadAndGetURL(
    location: S3ObjectLocation,
    content: InputStreamWithLength,
    expiryLength: Duration,
    checkExists: Boolean
  ): Either[StorageError, URL] =
    for {
      exists <- location.exists

      _ <- if (!exists || !checkExists) {
        s3StreamStore.put(location)(content)
      } else {
        Right(Identified(location, content))
      }

      url <- getPresignedGetURL(location, expiryLength)
    } yield url

  def uploadAndGetURL(
    location: S3ObjectLocation,
    content: String,
    expiryLength: Duration,
    checkExists: Boolean = false
  ): Either[StorageError, URL] =
    for {
      inputStream <- stringCodec.toStream(content)
      result <- uploadAndGetURL(
        location = location,
        content = inputStream,
        expiryLength = expiryLength,
        checkExists = checkExists
      )
    } yield result

  def getPresignedGetURL(
    location: S3ObjectLocation,
    expiryLength: Duration
  ): Either[ReadError, URL] = {

    // Based on an example from the AWS SDK for Java docs:
    // https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURLJavaSDK.html
    val currentTime = new util.Date()
    val currTimeMillis = currentTime.getTime
    val expTimeMillis = currTimeMillis + expiryLength.toMillis
    val expTime = new util.Date(expTimeMillis)

    val request =
      new GeneratePresignedUrlRequest(location.bucket, location.key)
        .withMethod(HttpMethod.GET)
        .withExpiration(expTime)

    Try {
      s3Client.generatePresignedUrl(request)
    } match {
      case Success(url) => Right(url)
      case Failure(err) => Left(StoreReadError(err))
    }
  }
}
