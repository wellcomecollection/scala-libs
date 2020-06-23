package uk.ac.wellcome.storage.store

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.{Identified, ObjectLocation, ReadError, RetryOps}

import scala.util.{Failure, Success, Try}

trait RetryableReadable[T] extends Readable[ObjectLocation, T] with Logging {
  import RetryOps._

  val maxRetries: Int

  def retryableGetFunction(location: ObjectLocation): T

  def buildGetError(throwable: Throwable): ReadError

  def get(location: ObjectLocation): ReadEither =
    retryableGet(location) map { t =>
      Identified(location, t)
    }

  def retryableGet(location: ObjectLocation): Either[ReadError, T] =
    getOnce().retry(maxRetries)(location)

  private def getOnce(): ObjectLocation => Either[ReadError, T] =
    (location: ObjectLocation) =>
      Try {
        retryableGetFunction(location)
      } match {
        case Success(t) => Right(t)
        case Failure(err) =>
          val error = buildGetError(err)
          warn(s"Error when trying to get $location: $error")
          Left(error)
    }
}
