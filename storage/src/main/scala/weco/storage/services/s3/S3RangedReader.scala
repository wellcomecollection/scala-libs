package weco.storage.services.s3

import org.apache.commons.io.IOUtils
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import weco.storage.ReadError
import weco.storage.models.{ByteRange, ClosedByteRange, OpenByteRange}
import weco.storage.s3.{S3Errors, S3ObjectLocation}
import weco.storage.services.RangedReader

import scala.util.{Failure, Success, Try}

class S3RangedReader(implicit s3Client: S3Client)
    extends RangedReader[S3ObjectLocation] {
  override def getBytes(
    location: S3ObjectLocation,
    range: ByteRange
  ): Either[ReadError, Array[Byte]] =
    Try {

      // The S3 Range request is *inclusive* of the boundaries.
      //
      // For example, if you read (start=0, end=5), you get bytes [0, 1, 2, 3, 4, 5].
      // We never want to read more than bufferSize bytes at a time.
      val getRequest = range match {
        case ClosedByteRange(start, count) =>
          GetObjectRequest.builder()
            .bucket(location.bucket)
            .key(location.key)
            .range(s"bytes=$start-${start + count - 1}")
            .build()

        case OpenByteRange(start) =>
          GetObjectRequest.builder()
            .bucket(location.bucket)
            .key(location.key)
            .range(s"bytes=$start-")
            .build()
      }

      s3Client.getObjectAsBytes(getRequest).asByteArray()
    } match {
      case Success(bytes) => Right(bytes)
      case Failure(err)   => Left(S3Errors.readErrors(err))
    }
}
