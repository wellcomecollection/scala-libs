package weco.storage.locking.dynamo

import java.util.UUID
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar
import org.scanamo.generic.auto._
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{
  DeleteItemRequest,
  DynamoDbException,
  PutItemRequest,
  QueryRequest
}
import weco.storage.dynamo.DynamoTimeFormat._
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.locking.{
  LockDaoTestCases,
  LockFailure,
  UnlockFailure
}

import scala.concurrent.duration._
import scala.language.higherKinds

class DynamoLockDaoTest
    extends LockDaoTestCases[String, UUID, Table]
    with DynamoLockDaoFixtures
    with MockitoSugar
    with IntegrationPatience {

  private val staticId = createIdent
  private val staticContextId = createContextId

  describe("DynamoLockDao") {
    it("records a lock in DynamoDB") {
      withLocalDynamoDbTable { lockTable =>
        withLockDao(lockTable) { lockDao =>
          lockDao
            .lock(staticId, staticContextId)
            .value
            .id shouldBe staticId

          getExistingTableItem[ExpiringLock](staticId, table = lockTable).id shouldBe staticId
        }
      }
    }

    it("refreshes the expiry on an existing lock") {
      withLocalDynamoDbTable { lockTable =>
        withLockDao(lockTable) { lockDao =>
          lockDao
            .lock(staticId, staticContextId)
            .value
            .id shouldBe staticId

          val expiry = getExistingTableItem[ExpiringLock](
            staticId,
            table = lockTable).expires

          // Wait at least 1 second
          Thread.sleep(1000)

          lockDao
            .lock(staticId, staticContextId)
            .value
            .id shouldBe staticId

          val updatedExpiry =
            getExistingTableItem[ExpiringLock](staticId, table = lockTable).expires

          expiry.isBefore(updatedExpiry) shouldBe true
        }
      }
    }

    it(
      "creates a new lock in a different context when the existing lock expires") {
      withLocalDynamoDbTable { lockTable =>
        withLockDao(lockTable, expiryTime = 1.second) { lockDao =>
          val contextId = createContextId

          lockDao
            .lock(staticId, staticContextId)
            .value
            .id shouldBe staticId

          lockDao
            .lock(staticId, contextId)
            .left
            .value shouldBe a[LockFailure[_]]

          // Allow the existing lock to expire
          Thread.sleep(2000)

          // Confirm we can lock expired lock
          lockDao.lock(staticId, contextId).value.id shouldBe staticId
        }
      }
    }

    it("removes a lock from DynamoDB after unlocking") {
      withLocalDynamoDbTable { lockTable =>
        withLockDao(lockTable, expiryTime = 1.second) { lockDao =>
          lockDao.lock(staticId, staticContextId).value
          lockDao.unlock(staticContextId)
          assertNoLocks(lockTable)
        }
      }
    }

    it("can lock > 25 locks") {
      // 25 is more than the BatchSize supported for a single BatchWriteItem
      // operation in DynamoDB.
      withLocalDynamoDbTable { lockTable =>
        withLockDao(lockTable, expiryTime = 1.second) { lockDao =>
          (1 to 50).map { id =>
            lockDao.lock(id.toString, staticContextId).value
          }

          lockDao.unlock(staticContextId)
          assertNoLocks(lockTable)
        }
      }
    }

    describe("Locking problems") {
      it("fails if there is a problem writing the lock") {
        val mockClient = mock[DynamoDbClient]

        val putItem = mockClient.putItem(any[PutItemRequest])
        val error = DynamoDbException.builder().message("BOOM!").build()

        when(putItem).thenThrow(error)

        withLockDao(mockClient) { lockDao =>
          lockDao
            .lock(staticId, staticContextId)
            .left
            .value shouldBe LockFailure(staticId, error)
        }
      }
    }

    describe("Unlocking problems") {
      it("fails to read the context locks") {
        val mockClient = mock[DynamoDbClient]

        val query = mockClient.query(any[QueryRequest])
        val error = DynamoDbException.builder().message("BOOM!").build()

        when(query).thenThrow(error)

        withLockDao(mockClient) { lockDao =>
          lockDao.unlock(staticContextId).left.value shouldBe UnlockFailure(
            staticContextId,
            error)
        }
      }

      it("fails to delete the lock") {
        val mockClient = mock[DynamoDbClient]

        val error = DynamoDbException.builder().message("BOOM!").build()

        when(mockClient.query(any[QueryRequest]))
          .thenThrow(error)
        when(mockClient.deleteItem(any[DeleteItemRequest]))
          .thenThrow(error)

        withLockDao(mockClient) { lockDao =>
          lockDao.unlock(staticContextId).left.value shouldBe UnlockFailure(
            staticContextId,
            error)
        }
      }
    }
  }
}
