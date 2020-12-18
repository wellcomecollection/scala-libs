package uk.ac.wellcome.storage.s3

import java.net.SocketTimeoutException

import software.amazon.awssdk.services.s3.model.AmazonS3Exception
import uk.ac.wellcome.storage.{
  DoesNotExistError,
  ReadError,
  RetryableError,
  StoreReadError
}

object S3Errors {
  val readErrors: PartialFunction[Throwable, ReadError] = {
    case exc: AmazonS3Exception if exc.getStatusCode == 404 =>
      DoesNotExistError(exc)

    case exc: AmazonS3Exception if exc.getStatusCode == 500 =>
      new StoreReadError(exc) with RetryableError

    case exc: AmazonS3Exception
        if exc.getMessage.startsWith("The specified bucket is not valid") =>
      StoreReadError(exc)

    case exc: SocketTimeoutException =>
      new StoreReadError(exc) with RetryableError

    case exc => StoreReadError(exc)
  }
}
