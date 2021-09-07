package weco.storage.store

import grizzled.slf4j.Logging
import weco.storage._
import weco.storage.maxima.Maxima

import scala.util.{Failure, Success, Try}

class VersionedStore[Id, V, T](
  val store: Store[Version[Id, V], T] with Maxima[Id, Version[Id, V], T]
)(implicit N: Numeric[V], O: Ordering[V])
    extends Store[Version[Id, V], T]
    with Logging {

  type StorageEither = Either[StorageError, Identified[Version[Id, V], T]]
  type UpdateEither = Either[UpdateError, Identified[Version[Id, V], T]]
  type UpdateFunctionEither = Either[UpdateFunctionError, T]

  type UpdateFunction = T => UpdateFunctionEither

  private val zero = N.zero
  private def increment(v: V): V = N.plus(v, N.one)

  private def nextVersionFor(id: Id): Either[ReadError, V] =
    store
      .max(id)
      .map { case Identified(Version(_, version), _) => increment(version) }

  private val matchErrors: PartialFunction[
    StorageEither,
    UpdateEither
  ] = {
    case Left(err: NoVersionExistsError) =>
      Left(UpdateNoSourceError(err))

    case Left(err: ReadError) =>
      Left(UpdateReadError(err))

    // We need to handle the case where two processes call update() simultaneously,
    // and the version checking logic in put() throws an error.
    //
    // See VersionedStoreRaceConditionsTest for examples of how this can occur.
    case Left(err: VersionAlreadyExistsError) =>
      Left(new UpdateWriteError(err) with RetryableError)
    case Left(err: HigherVersionExistsError) =>
      Left(new UpdateWriteError(err) with RetryableError)

    case Left(err: WriteError) =>
      Left(UpdateWriteError(err))

    case Left(err: UpdateError) =>
      Left(err)

    case Right(r) =>
      Right(r)
  }

  private def safeF(f: UpdateFunction)(t: T): UpdateFunctionEither =
    Try(f(t)) match {
      case Success(value) => value
      case Failure(e)     => Left(UpdateUnexpectedError(e))
    }

  def init(id: Id)(t: T): WriteEither =
    put(Version(id, N.zero))(t)

  def upsert(id: Id)(t: T)(f: UpdateFunction): UpdateEither =
    update(id)(f) match {
      case Left(UpdateNoSourceError(_)) =>
        matchErrors.apply(put(Version(id, N.zero))(t))
      case default => default
    }

  def update(id: Id)(f: UpdateFunction): UpdateEither =
    matchErrors.apply(for {
      latest <- getLatest(id)
      updatedT <- safeF(f)(latest.identifiedT)
      result <- put(Version(id, increment(latest.id.version)))(updatedT)
    } yield result)

  def get(id: Version[Id, V]): ReadEither =
    store.get(id) match {
      case r @ Right(_) => r
      case Left(DoesNotExistError(_)) =>
        Left(NoVersionExistsError())
      case Left(err) => Left(err)
    }

  def getLatest(id: Id): ReadEither =
    store.max(id).left.map {
      case NoMaximaValueError(_) => NoVersionExistsError()
      case err                   => err
    }

  def put(id: Version[Id, V])(t: T): WriteEither =
    store.max(id.id) match {
      // If you try to write a value that's already stored, that's fine.
      case Right(latest) if O.gteq(latest.id.version, id.version) && isAlreadyStored(id, t) =>
        Right(Identified(id, t))

      case Right(latest) if O.gt(latest.id.version, id.version) =>
        Left(HigherVersionExistsError())
      case Right(latest) if latest.id.version == id.version =>
        Left(VersionAlreadyExistsError())
      case _ =>
        store.put(id)(t)
    }

  private def isAlreadyStored(id: Version[Id, V], t: T): Boolean =
    store.get(id) match {
      case Right(Identified(_, storedT)) => t == storedT
      case _                             => false
    }

  def putLatest(id: Id)(t: T): WriteEither = {
    val result = nextVersionFor(id) match {
      case Right(v) =>
        put(Version(id, v))(t)
      case Left(NoMaximaValueError(_)) =>
        put(Version(id, zero))(t)
      case Left(err: StorageError) =>
        Left(StoreWriteError(err.e))
    }

    result match {
      case Right(value) => Right(value)

      // We need to handle the case where two processes call putLatest() simultaneously,
      // and the version checking logic in put() throws an error.
      //
      // See VersionedStoreRaceConditionsTest for examples of how this can occur.
      case Left(_: VersionAlreadyExistsError) | Left(
            _: HigherVersionExistsError) =>
        Left(
          new StoreWriteError(
            new Throwable(s"Another process wrote to id=$id simultaneously"))
          with RetryableError
        )

      case Left(err) => Left(err)
    }
  }
}
