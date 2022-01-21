package weco.messaging

sealed trait MessagingError {
  val e: Throwable
}

sealed trait MessageSenderError extends MessagingError

object MessageSenderError {
  case class JsonEncodingError(e: Throwable) extends MessageSenderError
  case class DestinationDoesNotExist(e: Throwable) extends MessageSenderError
  case class UnknownError(e: Throwable) extends MessageSenderError
}
