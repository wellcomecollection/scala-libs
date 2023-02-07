package weco.storage.store.s3

import grizzled.slf4j.Logging
import org.apache.commons.io.FileUtils
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._
import weco.storage._
import weco.storage.s3.{S3Errors, S3ObjectLocation}
import weco.storage.store.Writable
import weco.storage.streaming.InputStreamWithLength

import scala.util.{Failure, Success, Try}

trait S3StreamWritable
    extends Writable[S3ObjectLocation, InputStreamWithLength]
    with S3MultipartUploader
    with Logging {
  implicit val s3Client: S3Client
  val partSize: Long

  require(
    partSize >= 5 * FileUtils.ONE_MB,
    s"Parts must be at least 5 MB in size, got $partSize < ${5 * FileUtils.ONE_MB}; see https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html"
  )

  override def put(location: S3ObjectLocation)(
    inputStream: InputStreamWithLength): WriteEither = {
    val result =
      if (inputStream.length <= partSize) {
        val putObjectRequest =
          PutObjectRequest
            .builder()
            .bucket(location.bucket)
            .key(location.key)
            .build()

        for {
          requestBody <- Try {
            if (inputStream.length > 0) {
              val bytes: Array[Byte] = new Array[Byte](inputStream.length.toInt)
              val bytesRead =
                inputStream.read(bytes, 0, inputStream.length.toInt)

              if (bytesRead < inputStream.length) {
                throw new RuntimeException(
                  s"Input stream is too short: tried to read ${inputStream.length} bytes, only got $bytesRead"
                )
              }

              if (inputStream.available() > 0) {
                throw new RuntimeException(
                  s"Not all bytes read from input stream: read ${inputStream.length} bytes, but ${inputStream
                    .available()} bytes still available")
              }

              RequestBody.fromBytes(bytes)
            } else {
              RequestBody.empty()
            }
          }
          _ <- Try { s3Client.putObject(putObjectRequest, requestBody) }
        } yield ()
      } else {
        for {
          uploadId <- createMultipartUpload(location)
          completedParts <- uploadParts(uploadId, location, inputStream)
          _ <- completeMultipartUpload(location, uploadId, completedParts)
        } yield ()
      }

    result match {
      case Success(_) => Right(Identified(location, inputStream))
      case Failure(e) => Left(buildPutError(e))
    }
  }

  private def uploadParts(
    uploadId: String,
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength
  ): Try[List[CompletedPart]] = {
    val partCount = (inputStream.length.toFloat / partSize).ceil.toInt

    // part numbers in MultiPart uploads are 1-indexed
    val result = Range(1, partCount + 1).map { partNumber =>
      // We need to know how many bytes to read from the InputStream for
      // this part; remember that the final part may be shorter than the
      // other parts.
      val start = (partNumber - 1) * partSize
      val end = Math.min(partNumber * partSize, inputStream.length)
      val partLength = (end - start).toInt

      val bytes: Array[Byte] = new Array[Byte](partLength)
      val bytesRead = inputStream.read(bytes, 0, partLength)

      if (bytesRead < partLength) {
        throw new RuntimeException(
          s"Input stream is too short: tried to read $partLength bytes, only got $bytesRead"
        )
      }

      if (partNumber == partCount && inputStream.available() > 0) {
        throw new RuntimeException(
          s"Not all bytes read from input stream: read ${inputStream.length} bytes, but ${inputStream
            .available()} bytes still available")
      }

      uploadPart(location, uploadId, bytes, partNumber)
    }.toList

    val successes = result.collect { case Success(s)     => s }
    val failures = result.collectFirst { case Failure(e) => e }

    failures match {
      case None    => Success(successes)
      case Some(e) => Failure(e)
    }
  }

  private def buildPutError(throwable: Throwable): WriteError =
    throwable match {
      case exc: RuntimeException
          if exc.getMessage.startsWith(
            "Not all bytes read from input stream") =>
        IncorrectStreamLengthError(exc)
      case exc: RuntimeException
          if exc.getMessage.startsWith("Input stream is too short") =>
        IncorrectStreamLengthError(exc)

      // e.g. Request content was only 1024 bytes, but the specified content-length was 1025 bytes.
      case exc: SdkClientException
          if exc.getCause
            .isInstanceOf[IllegalStateException] && exc.getCause.getMessage
            .contains("Request content was only") && exc.getCause.getMessage
            .contains("but the specified content-length was") =>
        IncorrectStreamLengthError(exc)

      case _ => S3Errors.writeErrors(throwable)
    }
}
