package weco.storage.locking.dynamo

import java.util.UUID
import cats.data.EitherT
import cats.implicits._
import grizzled.slf4j.Logging
import org.scanamo.generic.semiauto.deriveDynamoFormat
import org.scanamo.query.Condition
import org.scanamo.syntax._
import org.scanamo.{DynamoFormat, Table => ScanamoTable}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import weco.storage.locking.{LockDao, LockFailure, UnlockFailure}
import weco.storage.dynamo.DynamoTimeFormat._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class DynamoLockDao(
  val client: DynamoDbClient,
  config: DynamoLockDaoConfig
)(implicit val ec: ExecutionContext)
    extends LockDao[String, UUID]
    with Logging
    with ScanamoHelpers[ExpiringLock] {

  implicit val df: DynamoFormat[ExpiringLock] =
    deriveDynamoFormat[ExpiringLock]

  override val table: ScanamoTable[ExpiringLock] =
    ScanamoTable[ExpiringLock](config.dynamoConfig.tableName)

  override val index: String = config.dynamoConfig.indexName

  // Lock

  override def lock(id: Ident, contextId: ContextId): LockResult = {
    val rowLock = ExpiringLock.create(
      id = id,
      contextId = contextId,
      duration = config.expiryTime
    )

    debug(s"Locking $id/$contextId: START")

    lockOp(rowLock).fold(
      e => {
        debug(s"Locking $id/$contextId: FAILURE ($e)")
        Left(LockFailure(id, e))
      },
      result => {
        debug(s"Locking $id/$contextId: SUCCESS ($result)")
        Right(rowLock)
      }
    )
  }

  private def lockOp(lock: ExpiringLock): Either[Throwable, Unit] = {
    val lockFound = attributeExists("id")
    val lockNotFound = not(lockFound)

    val isExpired = Condition("expires" < lock.created)

    val lockHasExpired = Condition(lockFound and isExpired)

    val lockAlreadyExists = Condition("contextId" === lock.contextId)

    val canLock =
      lockHasExpired or lockNotFound or lockAlreadyExists

    Try { scanamo.exec(table.when(canLock).put(lock)) } match {
      case Success(Right(_)) => Right(())
      case Success(Left(err)) =>
        Left(new Throwable(s"Error from Scanamo: $err"))
      case Failure(err) => Left(err)
    }
  }

  // Unlock

  override def unlock(contextId: ContextId): UnlockResult = {
    debug(s"Unlocking $contextId: START")

    queryAndDelete(contextId).fold(
      e => {
        warn(s"Unlocking $contextId: FAILURE ($e)")
        Left(UnlockFailure(contextId, e))
      },
      result => {
        trace(s"Unlocking $contextId: SUCCESS ($result)")
        Right(result)
      }
    )
  }

  private def queryAndDelete(contextId: ContextId): Either[Throwable, Unit] =
    for {
      queryOp <- queryLocks(contextId).toEither
      rowLocks <- queryOp
      _ <- deleteLocks(rowLocks)
    } yield ()

  private def deleteLocks(
    rowLocks: List[ExpiringLock]): Either[Throwable, Unit] =
    Try {
      val ids = rowLocks.map { _.id }.toSet
      val ops = table.deleteAll("id" in ids)
      scanamo.exec(ops)
    }.map { _ =>
      ()
    }.toEither

  private def queryLocks(contextId: ContextId) = Try {
    val queryT = EitherT(
      scanamo.exec(table.index(index).query("contextId" === contextId))
    )

    val readErrors = queryT.swap.collectRight
    val rowLocks = queryT.collectRight

    if (readErrors.isEmpty) {
      Right(rowLocks)
    } else {
      Left(new Error(s"Querying $contextId failed with $readErrors"))
    }
  }
}
