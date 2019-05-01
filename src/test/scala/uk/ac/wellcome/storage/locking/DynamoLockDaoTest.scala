package uk.ac.wellcome.storage.locking

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{EitherValues, FunSpec, Matchers}
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LockingFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.runtime.BoxedUnit
import scala.util.Random

class DynamoLockDaoTest
  extends FunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with LockingFixtures
    with EitherValues
    with PatienceConfiguration {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(40, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  case class ThingToStore(id: String, value: String)

  private val staticId = "staticId"
  private val staticContextId = "staticContextId"

  def createTable(table: Table): Table =
    createLockTable(table)

  it("locks a thing") {
    withLocalDynamoDbTable { lockTable =>
      withLockDao(lockTable) { lockDao =>

        lockDao.lock(staticId, staticContextId)
          .right.value.id shouldBe staticId

        getDynamo(lockTable)(staticId)
          .id shouldBe staticId
      }
    }
  }

  it("can expand a locked context set") {
    withLockDao { lockDao =>

      val id1 = createRandomId
      val id2 = createRandomId

      lockDao.lock(id1, staticContextId)
        .right.value.id shouldBe id1

      lockDao.lock(id2, staticContextId)
        .right.value.id shouldBe id2
    }

  }

  it("can refresh an existing lock") {
    withLocalDynamoDbTable { lockTable =>
      withLockDao(lockTable) { lockDao =>

        lockDao.lock(staticId, staticContextId)
          .right.value.id shouldBe staticId

        val expiry =
          getDynamo(lockTable)(staticId).expires

        // Wait at least 1 second
        Thread.sleep(1000)

        lockDao.lock(staticId, staticContextId)
          .right.value.id shouldBe staticId

        val updatedExpiry =
          getDynamo(lockTable)(staticId).expires

        expiry.isBefore(updatedExpiry) shouldBe true
      }
    }
  }

  it("cannot lock a locked context") {
    withLockDao { lockDao =>

      lockDao.lock(staticId, staticContextId)
        .right.value.id shouldBe staticId

      lockDao.lock(staticId, createRandomContextId)
        .left.value shouldBe a[LockFailure[_]]
    }

  }

  it("can lock a locked context that has expired") {
    withLocalDynamoDbTable { lockTable =>
      withLockDao(lockTable) { lockDao =>
        val contextId = createRandomContextId

        lockDao.lock(staticId, staticContextId)
          .right.value.id shouldBe staticId

        lockDao.lock(staticId, contextId)
          .left.value shouldBe a[LockFailure[_]]

        expireLock(lockTable)(staticId)

        // Confirm we can lock expired lock
        lockDao.lock(staticId, staticContextId)
          .right.value.id shouldBe staticId
      }
    }
  }

  it("unlocks a locked context and can re-lock") {
    withLockDao { lockDao =>
      val id = Random.nextString(32)

      lockDao.lock(id, staticContextId)
        .right.value.id shouldBe id

      lockDao.unlock(staticContextId)
        .right.value shouldBe a[BoxedUnit]

      lockDao.lock(id, staticContextId)
        .right.value.id shouldBe id

    }
  }

  describe("Locking problem:") {
    it("fails if there is a problem writing the lock") {
      val mockClient = mock[AmazonDynamoDB]

      val id = Random.nextString(9)

      val putItem = mockClient.putItem(any[PutItemRequest])
      val error = new InternalServerErrorException("FAILED")

      when(putItem).thenThrow(error)

      withLocalDynamoDbTable { lockTable =>
        withLockDao(mockClient, lockTable) { lockDao =>

          lockDao.lock(id, staticContextId)
            .left.value shouldBe LockFailure(id, error)

        }
      }
    }
  }

  describe("Unlocking problem:") {
    it("fails to read the context locks") {
      val mockClient = mock[AmazonDynamoDB]

      val contextId = Random.nextString(9)

      val query = mockClient.query(any[QueryRequest])
      val error = new InternalServerErrorException("FAILED")

      when(query).thenThrow(error)

      withLocalDynamoDbTable { lockTable =>
        withLockDao(mockClient, lockTable) { lockDao =>

          lockDao.unlock(contextId)
            .left.value shouldBe UnlockFailure(contextId, error)

        }
      }
    }

    it("fails to delete the lock") {
      val mockClient = mock[AmazonDynamoDB]

      val contextId = Random.nextString(9)

      val error = new InternalServerErrorException("FAILED")

      when(mockClient.query(any[QueryRequest]))
        .thenThrow(error)
      when(mockClient.deleteItem(any[DeleteItemRequest]))
        .thenThrow(error)

      withLocalDynamoDbTable { lockTable =>
        withLockDao(mockClient, lockTable) { lockDao =>

          lockDao.unlock(contextId)
            .left.value shouldBe UnlockFailure(contextId, error)

        }
      }
    }
  }

  it("allows one success if multiple processes lock a thing") {
    withLockDao { lockDao =>
      val lockUnlockCycles = 5
      val parallelism = 5

      // All locks/unlocks except one will fail in each cycle
      val expectedFail = parallelism - 1

      (1 to lockUnlockCycles).foreach { _ =>
        val id = createRandomId
        val countDownLatch = new CountDownLatch(parallelism)

        val eventualLocks = Future.sequence {
          (1 to parallelism).map { _ =>
            Future {
              countDownLatch.countDown()
              lockDao.lock(id, createRandomContextId)
            }
          }
        }

        countDownLatch.await(5, TimeUnit.SECONDS)

        whenReady(eventualLocks) { lockAttempts =>
          lockAttempts.count(_.isRight) shouldBe 1
          lockAttempts.count(_.isLeft) shouldBe expectedFail
        }
      }
    }
  }
}
