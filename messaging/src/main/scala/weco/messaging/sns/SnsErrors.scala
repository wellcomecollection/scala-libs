package weco.messaging.sns

import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sns.model.SnsException
import weco.errors.RetryableError
import weco.messaging.MessageSenderError

object SnsErrors {
  val sendErrors: PartialFunction[Throwable, MessageSenderError] = {
    case exc: SnsException if exc.statusCode() == 404 =>
      MessageSenderError.DestinationDoesNotExist(exc)

    case exc: SnsException if exc.statusCode() == 500 =>
      new MessageSenderError.UnknownError(exc) with RetryableError

    case exc: SdkClientException
        if exc.getMessage.startsWith("Unable to execute HTTP request") =>
      new MessageSenderError.UnknownError(exc) with RetryableError

    case exc: SdkClientException
        if exc.getMessage.startsWith(
          "Received an UnknownHostException when attempting to interact with a service") =>
      new MessageSenderError.UnknownError(exc) with RetryableError

    case exc => MessageSenderError.UnknownError(exc)
  }
}
