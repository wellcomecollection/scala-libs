package weco.storage.s3

import com.amazonaws.{SdkClientException => OldSdkClientException}
import com.amazonaws.services.s3.model.AmazonS3Exception
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.model.S3Exception
import weco.storage.{DoesNotExistError, ReadError, RetryableError, StoreReadError, StoreWriteError, WriteError}

import java.net.{SocketTimeoutException, UnknownHostException}

object S3Errors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: AmazonS3Exception if exc.getStatusCode == 404 =>
      DoesNotExistError(exc)
    case exc: S3Exception if exc.statusCode() == 404 =>
      DoesNotExistError(exc)

    case exc: AmazonS3Exception if exc.getStatusCode == 500 =>
      new StoreReadError(exc) with RetryableError
    case exc: S3Exception if exc.statusCode() == 500 =>
      new StoreReadError(exc) with RetryableError

    // The full error message here is:
    //
    //    Your socket connection to the server was not read from or written to
    //    within the timeout period. Idle connections will be closed.
    //
    case exc: AmazonS3Exception
        if exc.getMessage.startsWith(
          "Your socket connection to the server was not read from or written to within the timeout period") =>
      new StoreReadError(exc) with RetryableError

    case exc: AmazonS3Exception
        if exc.getMessage.startsWith("The specified bucket is not valid") =>
      StoreReadError(exc)

    case exc: OldSdkClientException
        if exc.getMessage.startsWith("Unable to execute HTTP request") =>
      new StoreReadError(exc) with RetryableError
    case exc: SdkClientException
      if exc.getMessage.startsWith("Unable to execute HTTP request") =>
      new StoreReadError(exc) with RetryableError
    case exc: SdkClientException if exc.getCause.isInstanceOf[UnknownHostException] =>
      new StoreReadError(exc) with RetryableError

    case exc: SocketTimeoutException =>
      new StoreReadError(exc) with RetryableError

    case exc => StoreReadError(exc)
  }

  val writeErrors: PartialFunction[Throwable, WriteError] = {
    case exc: AmazonS3Exception if exc.getStatusCode == 500 =>
      new StoreWriteError(exc) with RetryableError
    case exc: S3Exception if exc.statusCode() == 500 =>
      new StoreWriteError(exc) with RetryableError

    case exc => StoreWriteError(exc)
  }
}
