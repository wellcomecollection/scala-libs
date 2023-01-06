package weco.storage.store.s3

import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.UploadRequest
import weco.storage._
import weco.storage.s3.{S3Errors, S3ObjectLocation}
import weco.storage.store.Writable
import weco.storage.streaming.InputStreamWithLength

import java.util.concurrent.{CompletionException, Executors}
import scala.util.{Failure, Success, Try}

trait S3StreamWritable
    extends Writable[S3ObjectLocation, InputStreamWithLength] {
  implicit val transferManager: S3TransferManager

  // Maximum length of an s3 key is 1024 bytes as of 25/06/2019
  // https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
  private val MAX_KEY_BYTE_LENGTH = 1024

  private def createUploadRequest(
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength,
  ): Either[WriteError, UploadRequest] = {
    val keyByteLength = location.key.getBytes.length

    val putRequest =
      PutObjectRequest
        .builder()
        .bucket(location.bucket)
        .key(location.key)
        .contentLength(inputStream.length)
        .build()

    val requestBody =
      AsyncRequestBody.fromInputStream(
        inputStream,
        inputStream.length,
        Executors.newSingleThreadExecutor()
      )

    val uploadRequest =
      UploadRequest
        .builder()
        .putObjectRequest(putRequest)
        .requestBody(requestBody)
        .build()

    Either.cond(
      keyByteLength <= MAX_KEY_BYTE_LENGTH,
      uploadRequest,
      InvalidIdentifierFailure(
        new Error(
          s"S3 object key byte length is too big: $keyByteLength > $MAX_KEY_BYTE_LENGTH")
      )
    )
  }

  private def uploadWithTransferManager(
    uploadRequest: UploadRequest,
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength
  ): Either[WriteError, Identified[S3ObjectLocation, InputStreamWithLength]] =
    Try {
      transferManager
        .upload(uploadRequest)
        .completionFuture()
        .join()
    } match {
      case Success(_) if inputStream.available() > 0 =>
        Left(
          IncorrectStreamLengthError(
            new RuntimeException(
              "Data read has a different length than the expected")))
      case Success(_) => Right(Identified(location, inputStream))
      case Failure(err: CompletionException) =>
        Left(buildPutError(err.getCause))
      case Failure(err) => Left(buildPutError(err))
    }

  override def put(location: S3ObjectLocation)(
    inputStream: InputStreamWithLength): WriteEither =
    for {
      uploadRequest <- createUploadRequest(location, inputStream)
      result <- uploadWithTransferManager(uploadRequest, location, inputStream)
    } yield result

  private def buildPutError(throwable: Throwable): WriteError =
    throwable match {
      case exc: SdkClientException
          if exc.getMessage.startsWith(
            "Data read has a different length than the expected") =>
        IncorrectStreamLengthError(exc)
      case exc: SdkClientException
          if exc.getMessage.startsWith("More data read than expected") =>
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
