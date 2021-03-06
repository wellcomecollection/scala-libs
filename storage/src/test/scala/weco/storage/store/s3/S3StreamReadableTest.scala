package weco.storage.store.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import weco.storage.{DoesNotExistError, StoreReadError}
import weco.storage.fixtures.S3Fixtures

class S3StreamReadableTest extends AnyFunSpec with Matchers with S3Fixtures with EitherValues with MockitoSugar {
  def createS3ReadableWith(client: AmazonS3, retries: Int = 1): S3StreamReadable =
    new S3StreamReadable {
      override implicit val s3Client: AmazonS3 = client

      override val maxRetries: Int = retries
    }

  val s3ServerException = new AmazonS3Exception("We encountered an internal error. Please try again.")
  s3ServerException.setStatusCode(500)

  it("does not retry a deterministic error") {
    val spyClient = spy(s3Client)

    val readable = createS3ReadableWith(spyClient)

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)

      readable.get(location).left.value shouldBe a[DoesNotExistError]

      verify(spyClient, times(1)).getObject(location.bucket, location.key)
    }
  }

  it("retries a flaky error from S3") {
    val mockClient = mock[AmazonS3]

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)
      putStream(location)

      when(mockClient.getObject(any[String], any[String]))
        .thenThrow(s3ServerException)
        .thenReturn(s3Client.getObject(location.bucket, location.key))

      val readable = createS3ReadableWith(mockClient, retries = 3)
      readable.get(location) shouldBe a[Right[_, _]]

      verify(mockClient, times(2)).getObject(location.bucket, location.key)
    }
  }

  it("gives up if there are too many flaky errors") {
    val mockClient = mock[AmazonS3]

    val location = createS3ObjectLocation

    val retries = 4

    when(mockClient.getObject(any[String], any[String]))
      .thenThrow(s3ServerException)
      .thenThrow(s3ServerException)
      .thenThrow(s3ServerException)
      .thenThrow(s3ServerException)

    val readable = createS3ReadableWith(mockClient, retries = retries)
    readable.get(location).left.value shouldBe a[StoreReadError]

    verify(mockClient, times(retries)).getObject(location.bucket, location.key)
  }
}
