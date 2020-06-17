package uk.ac.wellcome.storage.s3

import java.net.SocketTimeoutException

import com.amazonaws.services.s3.model.AmazonS3Exception
import uk.ac.wellcome.storage.{
  DoesNotExistError,
  ReadError,
  RetryableError,
  StoreReadError
}

object S3Errors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: AmazonS3Exception
        if exc.getMessage.startsWith("The specified key does not exist") ||
          exc.getMessage.startsWith("The specified bucket does not exist") =>
      DoesNotExistError(exc)

    case exc: AmazonS3Exception
        if exc.getMessage.startsWith("The specified bucket is not valid") =>
      StoreReadError(exc)

    case exc: AmazonS3Exception
        if exc.getMessage.startsWith(
          "We encountered an internal error. Please try again.") ||
          exc.getMessage.startsWith("Please reduce your request rate.") =>
      new StoreReadError(exc) with RetryableError

    case exc: SocketTimeoutException =>
      new StoreReadError(exc) with RetryableError

    case exc => StoreReadError(exc)
  }
}
