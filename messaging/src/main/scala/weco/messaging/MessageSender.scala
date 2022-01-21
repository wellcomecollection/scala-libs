package weco.messaging

import grizzled.slf4j.Logging
import io.circe.Encoder
import weco.json.JsonUtil.toJson

import scala.util.{Failure, Success}

trait IndividualMessageSender[Destination] {
  type MessageSenderResult = Either[MessageSenderError, Unit]

  def send(body: String)(subject: String,
                         destination: Destination): MessageSenderResult

  def sendT[T](t: T)(subject: String, destination: Destination)(
    implicit encoder: Encoder[T]): MessageSenderResult =
    toJson(t) match {
      case Success(jsonString) => send(jsonString)(subject, destination)
      case Failure(e)          => Left(MessageSenderError.JsonEncodingError(e))
    }
}

trait MessageSender[Destination] extends Logging {
  type MessageSenderResult = Either[MessageSenderError, Unit]

  protected val underlying: IndividualMessageSender[Destination]

  val subject: String
  val destination: Destination

  def send(body: String): MessageSenderResult =
    underlying.send(body)(subject, destination) match {
      case Left(err) =>
        error(
          s"Unable to send message (body=$body) to destination $destination: $err",
          err.e)
        Left(err)

      case Right(_) => Right(())
    }

  def sendT[T](t: T)(implicit encoder: Encoder[T]): MessageSenderResult =
    underlying.sendT[T](t)(subject, destination) match {
      case Left(err) =>
        error(
          s"Unable to send message (t=$t) to destination $destination: $err",
          err.e)
        Left(err)

      case Right(_) => Right(())
    }
}
