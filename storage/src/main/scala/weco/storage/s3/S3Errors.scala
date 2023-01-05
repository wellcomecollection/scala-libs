package weco.storage.s3

import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.model.S3Exception
import weco.storage.{DoesNotExistError, ReadError, RetryableError, StoreReadError, StoreWriteError, WriteError}

import java.net.SocketTimeoutException

object S3Errors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: S3Exception if exc.statusCode() == 404 =>
      DoesNotExistError(exc)

    case exc: S3Exception if exc.statusCode() == 500 =>
      new StoreReadError(exc) with RetryableError

    // The full error message here is:
    //
    //    Your socket connection to the server was not read from or written to
    //    within the timeout period. Idle connections will be closed.
    //
    case exc: S3Exception
        if exc.getMessage.startsWith(
          "Your socket connection to the server was not read from or written to within the timeout period") =>
      new StoreReadError(exc) with RetryableError

    case exc: S3Exception
        if exc.getMessage.startsWith("The specified bucket is not valid") =>
      StoreReadError(exc)

    case exc: SdkClientException
        if exc.getMessage.startsWith("Unable to execute HTTP request") =>
      new StoreReadError(exc) with RetryableError

    case exc: SocketTimeoutException =>
      new StoreReadError(exc) with RetryableError

    case exc => StoreReadError(exc)
  }

  val writeErrors: PartialFunction[Throwable, WriteError] = {
    case exc: S3Exception if exc.statusCode() == 500 =>
      new StoreWriteError(exc) with RetryableError

    case exc => StoreWriteError(exc)
  }
}
