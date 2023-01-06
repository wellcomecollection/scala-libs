package weco.storage.store.s3

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, S3Exception}
import weco.storage.{DoesNotExistError, StoreReadError}
import weco.storage.fixtures.S3Fixtures

class S3StreamReadableTest
    extends AnyFunSpec
    with Matchers
    with S3Fixtures
    with EitherValues
    with MockitoSugar {
  def createS3ReadableWith(client: S3Client,
                           retries: Int = 1): S3StreamReadable =
    new S3StreamReadable {
      override implicit val s3Client: S3Client = client

      override val maxRetries: Int = retries
    }

  val s3ServerException = S3Exception.builder()
    .message("We encountered an internal error. Please try again.")
    .statusCode(500)
    .build()

  it("does not retry a deterministic error") {
    val mockClient = mock[S3Client]

    val readable = createS3ReadableWith(mockClient)

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)

      when(mockClient.getObject(any[GetObjectRequest]))
        .thenThrow(
          S3Exception.builder()
            .statusCode(404)
            .build()
        )

      readable.get(location).left.value shouldBe a[DoesNotExistError]

      verify(mockClient, times(1))
        .getObject(any[GetObjectRequest])
    }
  }

  it("retries a flaky error from S3") {
    val mockClient = mock[S3Client]

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)
      putStream(location)

      when(mockClient.getObject(any[GetObjectRequest]))
        .thenThrow(s3ServerException)
        .thenReturn({
          val getRequest =
            GetObjectRequest.builder()
              .bucket(location.bucket)
              .key(location.key)
              .build()

          s3ClientV2.getObject(getRequest)
        })

      val readable = createS3ReadableWith(mockClient, retries = 3)
      readable.get(location) shouldBe a[Right[_, _]]

      verify(mockClient, times(2)).getObject(any[GetObjectRequest])
    }
  }

  it("gives up if there are too many flaky errors") {
    val mockClient = mock[S3Client]

    val location = createS3ObjectLocation

    val retries = 4

    when(mockClient.getObject(any[GetObjectRequest]))
      .thenThrow(s3ServerException)
      .thenThrow(s3ServerException)
      .thenThrow(s3ServerException)
      .thenThrow(s3ServerException)

    val readable = createS3ReadableWith(mockClient, retries = retries)
    readable.get(location).left.value shouldBe a[StoreReadError]

    verify(mockClient, times(retries)).getObject(any[GetObjectRequest])
  }

  it("retries if it's unable to connect to S3") {
    val mockClient = mock[S3Client]

    val retries = 4
    val readable = createS3ReadableWith(mockClient, retries = retries)

    // TODO: We don't need a real bucket here
    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)

      val exception = SdkClientException.builder().message("Unable to execute HTTP request").build()

      when(mockClient.getObject(any[GetObjectRequest]))
        .thenThrow(exception)
        .thenThrow(exception)
        .thenThrow(exception)
        .thenThrow(exception)
        .thenThrow(exception)

      readable.get(location).left.value shouldBe a[StoreReadError]

      verify(mockClient, times(retries)).getObject(any[GetObjectRequest])
    }
  }
}
