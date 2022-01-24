package weco.messaging.worker.steps

import scala.util.{Failure, Success, Try}

trait MessageTransform[Message, Work] {
  protected val transform: Message => Either[Throwable, Work]

  final def callTransform(message: Message): Either[Throwable, Work] =
    Try(transform(message)) match {
      case Failure(e)    => Left(e)
      case Success(work) => work
    }
}
