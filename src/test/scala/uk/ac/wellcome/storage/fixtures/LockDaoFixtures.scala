package uk.ac.wellcome.storage.fixtures

import grizzled.slf4j.Logging
import uk.ac.wellcome.storage.locking.{LockDao, LockFailure, RowLock}

import java.time.Duration

trait LockDaoFixtures extends Logging {
  def createInMemoryLockDao = new LockDao[String, String] {

    var locks: Map[String, RowLock] = Map.empty

    private def createRowLock(id: String, ctxId: String) =
      RowLock.create(id, ctxId, Duration.ofDays(1))

    override def unlock(ctxId: String): Right[Nothing, Unit] = {
      info(s"Unlocking for $ctxId")
      locks = locks.filter { case (id, RowLock(_, ctx, _,_)) =>
        info(s"Unlocking $id")

        ctx != ctxId
      }

      Right(())
    }

    override def lock(id: String, ctxId: String): Either[LockFailure[String], RowLock] = {
      info(s"Locking $id, with $ctxId")

      locks.get(id) match {
        case Some(r@RowLock(_, ctx, _,_)) if ctxId == ctx => Right(r)
        case Some(RowLock(_, ctx, _,_)) if ctxId != ctx => Left(
          LockFailure[String](id,
            new Throwable(
            s"Failed lock $id in $ctxId, locked: $ctx"
          ))
        )
        case None =>
          val rowLock = createRowLock(id, ctxId)
          locks = locks ++ Map(id -> rowLock)
          Right(rowLock)
      }
    }
  }
}
