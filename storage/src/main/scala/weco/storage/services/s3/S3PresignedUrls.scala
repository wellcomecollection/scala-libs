package weco.storage.services.s3

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import weco.storage.s3.S3ObjectLocation
import weco.storage.{ReadError, StoreReadError}

import java.net.URL
import java.util
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

class S3PresignedUrls(implicit s3Client: AmazonS3) {
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
