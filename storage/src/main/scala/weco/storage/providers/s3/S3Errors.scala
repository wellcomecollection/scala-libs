package weco.storage.providers.s3

import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.model.S3Exception
import weco.storage._

import java.net.{SocketException, SocketTimeoutException, UnknownHostException}

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

    case exc: S3Exception
        if exc.getMessage.startsWith("The specified bucket does not exist") =>
      StoreReadError(exc)

    case exc: SdkClientException
        if exc.getMessage.startsWith("Unable to execute HTTP request") =>
      new StoreReadError(exc) with RetryableError

    case exc: SdkClientException
        if exc.getCause.isInstanceOf[UnknownHostException] =>
      new StoreReadError(exc) with RetryableError

    // e.g. java.net.SocketException: Connection reset
    case exc: SocketException =>
      new StoreReadError(exc) with RetryableError

    case exc: SocketTimeoutException =>
      new StoreReadError(exc) with RetryableError

    case exc => StoreReadError(exc)
  }

  val writeErrors: PartialFunction[Throwable, WriteError] = {
    case exc: S3Exception if exc.statusCode() == 500 =>
      new StoreWriteError(exc) with RetryableError

    // e.g. S3Exception: Object key is too long. Maximum number of bytes allowed in keys is 915.
    case exc: S3Exception
        if exc.getMessage.startsWith("Object key is too long") =>
      InvalidIdentifierFailure(exc)
    case exc: S3Exception
        if exc.getMessage.startsWith("Your key is too long") =>
      InvalidIdentifierFailure(exc)

    case exc => StoreWriteError(exc)
  }
}
