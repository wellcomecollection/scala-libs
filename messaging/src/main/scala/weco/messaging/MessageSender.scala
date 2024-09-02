package weco.messaging

import grizzled.slf4j.Logging
import io.circe.Encoder
import weco.json.JsonUtil.toJson

import scala.util.{Failure, Success, Try}

trait IndividualMessageSender[Destination] {
  def send(body: String)(subject: String, destination: Destination): Try[Unit]

  def sendT[T](t: T)(subject: String, destination: Destination)(
    implicit encoder: Encoder[T]): Try[Unit] =
    toJson(t).flatMap { send(_)(subject, destination) }
}

trait MessageSender[Destination] extends Logging {
  protected val underlying: IndividualMessageSender[Destination]

  val subject: String
  val destination: Destination

  def send(body: String): Try[Unit] =
    underlying.send(body)(subject, destination) match {
      case Failure(err) =>
        error(s"Unable to send message (body=$body) to destination $destination: $err", err)
        Failure(err)

      case Success(_) => Success(())
    }

  def sendT[T](t: T)(implicit encoder: Encoder[T]): Try[Unit] =
    underlying.sendT[T](t)(subject, destination) match {
      case Failure(err) =>
        error(
          s"Unable to send message (t=$t) to destination $destination: $err",
          err)
        Failure(err)

      case Success(_) => Success(())
    }
}
