package weco.storage.locking

import cats._
import cats.data._
import grizzled.slf4j.Logging

import scala.annotation.tailrec
import scala.language.higherKinds

trait LockingService[Out, OutMonad[_], LockDaoImpl <: LockDao[_, _]]
    extends Logging {

  import cats.implicits._

  implicit val lockDao: LockDaoImpl

  type LockingServiceResult = Either[FailedLockingServiceOp, lockDao.ContextId]
  type Process = Either[FailedLockingServiceOp, Out]

  type OutMonadError = MonadError[OutMonad, Throwable]

  def withLocks(ids: Set[lockDao.Ident])(
    f: => OutMonad[Out]
  )(implicit m: OutMonadError): OutMonad[Process] = {
    val contextId: lockDao.ContextId = createContextId()

    val eitherT = for {
      contextId <- EitherT.fromEither[OutMonad](
        getLocks(ids = ids, contextId = contextId))

      out <- EitherT(safeF(contextId)(f))
    } yield out

    eitherT.value
  }

  protected def createContextId(): lockDao.ContextId

  def withLock(id: lockDao.Ident)(f: => OutMonad[Out])(
    implicit m: OutMonadError): OutMonad[Process] =
    withLocks(Set(id)) { f }

  private def safeF(contextId: lockDao.ContextId)(
    f: => OutMonad[Out]
  )(implicit monadError: OutMonadError): OutMonad[Process] = {
    val partialF = f.map(o => {
      debug(s"Processing $contextId (got $o)")
      unlock(contextId)
      Either.right[FailedLockingServiceOp, Out](o)
    })

    monadError.handleError(partialF) { e =>
      unlock(contextId)
      Either.left[FailedLockingServiceOp, Out](
        FailedProcess[lockDao.ContextId](contextId, e)
      )
    }
  }

  /** Lock the entire set of identifiers we were given.  If any of them fail,
    * unlock the entire context and report a failure.
    *
    */
  @tailrec
  private def getLocks(ids: Set[lockDao.Ident],
                       contextId: lockDao.ContextId): LockingServiceResult =
    // We lock the IDs one-by-one, but if any ID fails to lock, we skip
    // even trying to lock the remaining IDs.
    //
    // This reduces the amount of churn in the LockDao: we're skipping creating
    // and deleting a lock we know will never be used.
    if (ids.isEmpty) {
      Right(contextId)
    } else {
      lockDao.lock(ids.head, contextId) match {
        case Left(failure) =>
          // Remember to unlock any locks we've already acquired!  Otherwise retrying
          // this operation later is doomed to fail.
          unlock(contextId)

          Left(FailedLock(contextId, ids.tail.map(skipped) + failure))
        case Right(_) => getLocks(ids.tail, contextId)
      }
    }

  private def skipped[Ident](id: Ident): LockFailure[Ident] =
    LockFailure(
      id,
      e = new Throwable(
        s"Skipping locking $id; other IDs have already failed to lock"
      )
    )

  private def unlock(contextId: lockDao.ContextId): Unit =
    lockDao
      .unlock(contextId)
      .leftMap { error =>
        warn(s"Unable to unlock context $contextId fully: $error")
      }
}

sealed trait FailedLockingServiceOp

case class FailedLock[ContextId, Ident](contextId: ContextId,
                                        lockFailures: Set[LockFailure[Ident]])
    extends FailedLockingServiceOp

case class FailedUnlock[ContextId, Ident](contextId: ContextId,
                                          ids: List[Ident],
                                          e: Throwable)
    extends FailedLockingServiceOp

case class FailedProcess[ContextId](contextId: ContextId, e: Throwable)
    extends FailedLockingServiceOp
