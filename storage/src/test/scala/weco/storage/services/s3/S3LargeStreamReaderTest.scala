package weco.storage.services.s3

import com.amazonaws.services.s3.model.GetObjectRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import weco.fixtures.TestWith
import weco.storage.fixtures.S3Fixtures
import weco.storage.fixtures.S3Fixtures.Bucket
import weco.storage.s3.S3ObjectLocation
import weco.storage.services.{
  LargeStreamReader,
  LargeStreamReaderTestCases,
  RangedReader
}
import weco.storage.streaming.Codec.stringCodec

class S3LargeStreamReaderTest
    extends LargeStreamReaderTestCases[S3ObjectLocation, Bucket]
    with S3Fixtures {
  override def withNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  override def createIdentWith(bucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(bucket)

  override def writeString(location: S3ObjectLocation, contents: String): Unit =
    putString(location, contents)

  override def withLargeStreamReader[R](
    bufferSize: Long
  )(testWith: TestWith[LargeStreamReader[S3ObjectLocation], R]): R =
    testWith(new S3LargeStreamReader(bufferSize = bufferSize))

  override def withRangedReader[R](
    testWith: TestWith[RangedReader[S3ObjectLocation], R]
  ): R =
    testWith(new S3RangedReader())

  override def withLargeStreamReader[R](
    bufferSize: Long,
    rangedReaderImpl: RangedReader[S3ObjectLocation]
  )(testWith: TestWith[LargeStreamReader[S3ObjectLocation], R]): R =
    testWith(
      new S3LargeStreamReader(bufferSize = bufferSize) {
        override protected val rangedReader: RangedReader[S3ObjectLocation] =
          rangedReaderImpl
      }
    )

  it("makes multiple GetObject requests") {
    val bufferSize = 500

    withLocalS3Bucket { bucket =>
      val location = createS3ObjectLocationWith(bucket)
      putStream(
        location,
        inputStream = createInputStream(length = bufferSize * 2)
      )

      val spyClient = Mockito.spy(s3Client)
      val reader =
        new S3LargeStreamReader(bufferSize = bufferSize)(s3Client = spyClient)

      // Consume all the bytes from the stream, even if we don't look at them.
      val inputStream = reader.get(location).value.identifiedT
      stringCodec.fromStream(inputStream).value

      // One to get the size of the object, two to read the contents
      Mockito
        .verify(spyClient, Mockito.atLeast(1))
        .getObjectMetadata(location.bucket, location.key)

      Mockito
        .verify(spyClient, Mockito.atLeast(2))
        .getObject(any[GetObjectRequest])
    }
  }
}
