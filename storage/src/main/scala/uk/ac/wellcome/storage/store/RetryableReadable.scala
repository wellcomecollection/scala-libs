package uk.ac.wellcome.storage.store

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.{Identified, ReadError, RetryOps}

import scala.util.{Failure, Success, Try}

trait RetryableReadable[Ident, T] extends Readable[Ident, T] with Logging {
  import RetryOps._

  val maxRetries: Int

  def retryableGetFunction(ident: Ident): T

  def buildGetError(throwable: Throwable): ReadError

  def get(ident: Ident): ReadEither =
    retryableGet(ident) map { t =>
      Identified(ident, t)
    }

  def retryableGet(ident: Ident): Either[ReadError, T] =
    getOnce.retry(maxRetries)(ident)

  private def getOnce: Ident => Either[ReadError, T] =
    (ident: Ident) =>
      Try {
        retryableGetFunction(ident)
      } match {
        case Success(t) => Right(t)
        case Failure(err) =>
          val error = buildGetError(err)
          warn(s"Error when trying to get $ident: $error")
          Left(error)
    }
}
