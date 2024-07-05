package weco.storage.locking.dynamo

import java.util.UUID
import org.scalatest.EitherValues
import org.scanamo.generic.auto._
import weco.storage.fixtures.DynamoFixtures.Table
import weco.storage.locking.LockingServiceTestCases
class DynamoLockingServiceTest
    extends LockingServiceTestCases[String, UUID, Table]
    with DynamoLockDaoFixtures
    with EitherValues {
  override def getCurrentLocks(lockDao: LockDaoStub,
                               lockDaoContext: Table): Set[String] =
    scanTable[ExpiringLock](lockDaoContext)
      .map { _.value }
      .map { _.id }
      .toSet
}
