package weco.storage.s3

import com.amazonaws.SdkClientException
import com.amazonaws.services.s3.model.AmazonS3Exception
import weco.errors.RetryableError
import weco.storage.{DoesNotExistError, ReadError, StoreReadError, StoreWriteError, WriteError}

import java.net.SocketTimeoutException

object S3Errors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: AmazonS3Exception if exc.getStatusCode == 404 =>
      DoesNotExistError(exc)

    case exc: AmazonS3Exception if exc.getStatusCode == 500 =>
      new StoreReadError(exc) with RetryableError

    case exc: AmazonS3Exception
        if exc.getMessage.startsWith("The specified bucket is not valid") =>
      StoreReadError(exc)

    case exc: SdkClientException if exc.getMessage.startsWith("Unable to execute HTTP request") =>
      new StoreReadError(exc) with RetryableError

    case exc: SocketTimeoutException =>
      new StoreReadError(exc) with RetryableError

    case exc => StoreReadError(exc)
  }

  val writeErrors: PartialFunction[Throwable, WriteError] = {
    case exc: AmazonS3Exception if exc.getStatusCode == 500 =>
      new StoreWriteError(exc) with RetryableError

    case exc => StoreWriteError(exc)
  }
}
