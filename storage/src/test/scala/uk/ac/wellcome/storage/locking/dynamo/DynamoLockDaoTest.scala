package uk.ac.wellcome.storage.locking.dynamo

import java.util.UUID

import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.mockito.MockitoSugar
import org.scanamo.auto._
import uk.ac.wellcome.storage.dynamo.DynamoTimeFormat._
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.locking.{
  LockDaoTestCases,
  LockFailure,
  UnlockFailure
}

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
        withLockDao(lockTable, seconds = 1) { lockDao =>
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
        withLockDao(lockTable, seconds = 1) { lockDao =>
          lockDao.lock(staticId, staticContextId).value
          lockDao.unlock(staticContextId)
          assertNoLocks(lockTable)
        }
      }
    }

    describe("Locking problems") {
      it("fails if there is a problem writing the lock") {
        val mockClient = mock[DynamoDbClient]

        val putItem = mockClient.putItem(any[PutItemRequest])
        val error = new InternalServerErrorException("FAILED")

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
        val error = new InternalServerErrorException("FAILED")

        when(query).thenThrow(error)

        withLockDao(mockClient) { lockDao =>
          lockDao.unlock(staticContextId).left.value shouldBe UnlockFailure(
            staticContextId,
            error)
        }
      }

      it("fails to delete the lock") {
        val mockClient = mock[DynamoDbClient]

        val error = new InternalServerErrorException("FAILED")

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
