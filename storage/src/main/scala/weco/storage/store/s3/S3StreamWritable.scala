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

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait S3StreamWritable
    extends Writable[S3ObjectLocation, InputStreamWithLength]
    with Logging {
  implicit val s3Client: S3Client
  val partSize: Long

  require(
    partSize >= 5 * FileUtils.ONE_MB,
    s"Parts must be at least 5 MB in size, got $partSize < ${5 * FileUtils.ONE_MB}; see https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html"
  )

  override def put(location: S3ObjectLocation)(
    inputStream: InputStreamWithLength): WriteEither =
    Try {
      val createRequest =
        CreateMultipartUploadRequest
          .builder()
          .bucket(location.bucket)
          .key(location.key)
          .build()

      val createResponse = s3Client.createMultipartUpload(createRequest)

      debug(
        s"Got CreateMultipartUploadResponse with upload ID ${createResponse.uploadId()}"
      )

      val completedParts = uploadParts(createResponse, location, inputStream)

      val completedMultipartUpload =
        CompletedMultipartUpload
          .builder()
          .parts(completedParts.asJava)
          .build()

      val completeRequest =
        CompleteMultipartUploadRequest
          .builder()
          .bucket(location.bucket)
          .key(location.key)
          .uploadId(createResponse.uploadId())
          .multipartUpload(completedMultipartUpload)
          .build()

      s3Client.completeMultipartUpload(completeRequest)
    } match {
      case Success(_) => Right(Identified(location, inputStream))
      case Failure(e) => Left(buildPutError(e))
    }

  private def uploadParts(
    createResponse: CreateMultipartUploadResponse,
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength
  ): List[CompletedPart] = {
    val partCount = (inputStream.length.toFloat / partSize).ceil.toInt

    // part numbers in MultiPart uploads are 1-indexed
    Range(1, partCount + 1).map { partNumber =>
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
          s"Input stream is too short: tried to read $partLength bytes in part $partNumber, only got $bytesRead"
        )
      }

      if (partNumber == partCount && inputStream.available() > 0) {
        throw new RuntimeException(s"Not all bytes read from input stream: read ${inputStream.length} bytes, but ${inputStream.available()} bytes still available")
      }

      val uploadPartRequest =
        UploadPartRequest
          .builder()
          .bucket(location.bucket)
          .key(location.key)
          .uploadId(createResponse.uploadId())
          .partNumber(partNumber)
          .build()

      val requestBody = RequestBody.fromBytes(bytes)

      val uploadPartResponse =
        s3Client.uploadPart(uploadPartRequest, requestBody)

      CompletedPart
        .builder()
        .eTag(uploadPartResponse.eTag())
        .partNumber(partNumber)
        .build()
    }.toList
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
