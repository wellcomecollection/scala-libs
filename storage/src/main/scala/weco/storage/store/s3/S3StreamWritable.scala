package weco.storage.store.s3

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.transfer.{TransferManagerBuilder, Upload}
import weco.storage._
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.Writable
import weco.storage.streaming.InputStreamWithLength

import scala.util.{Failure, Success, Try}

trait S3StreamWritable
    extends Writable[S3ObjectLocation, InputStreamWithLength] {
  implicit val s3Client: AmazonS3

  private val transferManager = TransferManagerBuilder.standard
    .withS3Client(s3Client)
    .build

  // Set bufferReadLimit
  // To prevent 'com.amazonaws.ResetException: Failed to reset the request input stream' being thrown.
  // (see https://github.com/aws/aws-sdk-java/issues/427)
  // (and https://github.com/wellcometrust/platform/issues/3481)
  //
  // If a transfer fails, the AWS SDK retries by rewinding the input stream to the 'mark' set in the buffer at the start.
  // The 'ReadLimit' determines how far the stream can be rewound, if it is smaller than the bytes sent before an error
  // occurs the mark will be invalid and a ResetException is thrown.
  //
  // When using a BufferedInputStream up to 'ReadLimit' bytes are stored in memory, and this must be (at least one byte)
  // larger than the PUT.  The buffer grows dynamically up to this limit.
  //
  // For multipart PUT requests the size must be larger than each PART.
  //
  // To prevent this exception a constant maximum size is set
  // assuming a maximum file of 600GB PUT as 10,000 multipart requests
  // = 60MB ~ 100MB read limit
  // this is a generous estimate and should be sufficient,
  // also given x10 concurrent streams = 10x100MB = 1GB memory overhead which we are comfortable with.
  // This change was tested to reproduce the error with a proxy that dropped traffic to simulate S3 network failure.
  private val MB: Int = 1024 * 1024
  private val BUFFER_READ_LIMIT: Int = 100 * MB

  // Maximum length of an s3 key is 1024 bytes as of 25/06/2019
  // https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingMetadata.html
  private val MAX_KEY_BYTE_LENGTH = 1024

  private def createPutObjectRequest(
    location: S3ObjectLocation,
    stream: InputStreamWithLength,
  ): Either[WriteError, PutObjectRequest] = {
    val keyByteLength = location.key.getBytes.length

    val metadata = new ObjectMetadata()

    metadata.setContentLength(stream.length)

    val request = new PutObjectRequest(
      location.bucket,
      location.key,
      stream,
      metadata
    )

    request.getRequestClientOptions.setReadLimit(BUFFER_READ_LIMIT)

    Either.cond(
      keyByteLength <= MAX_KEY_BYTE_LENGTH,
      request,
      InvalidIdentifierFailure(
        new Error(
          s"S3 object key byte length is too big: $keyByteLength > $MAX_KEY_BYTE_LENGTH")
      )
    )
  }

  private def uploadWithTransferManager(
    putObjectRequest: PutObjectRequest,
    location: S3ObjectLocation,
    inputStream: InputStreamWithLength
  ): Either[WriteError, Identified[S3ObjectLocation, InputStreamWithLength]] =
    Try {
      val upload: Upload = transferManager
        .upload(putObjectRequest)

      upload.waitForUploadResult()
    } match {
      case Success(_)   => Right(Identified(location, inputStream))
      case Failure(err) => Left(buildPutError(err))
    }

  override def put(location: S3ObjectLocation)(
    inputStream: InputStreamWithLength): WriteEither =
    for {
      putObjectRequest <- createPutObjectRequest(location, inputStream)
      result <- uploadWithTransferManager(
        putObjectRequest,
        location,
        inputStream)
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
      case _ => StoreWriteError(throwable)
    }
}
