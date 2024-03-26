package weco.storage.store

import weco.Logging
import weco.storage.{Identified, ReadError, RetryOps}

import scala.util.{Failure, Success, Try}

trait RetryableReadable[Ident, T] extends Readable[Ident, T] with Logging {
  import RetryOps._

  val maxRetries: Int

  protected def retryableGetFunction(id: Ident): T

  protected def buildGetError(throwable: Throwable): ReadError

  def get(id: Ident): ReadEither =
    getOnce.retry(maxRetries)(id) map { t =>
      Identified(id, t)
    }

  private def getOnce: Ident => Either[ReadError, T] =
    (id: Ident) =>
      Try {
        retryableGetFunction(id)
      } match {
        case Success(t) => Right(t)
        case Failure(err) =>
          val error = buildGetError(err)
          warn(s"Error when trying to get $id: $error")
          Left(error)
    }
}
