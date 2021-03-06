package weco.storage.locking.memory

import java.util.UUID

import weco.storage.locking.LockingServiceTestCases

class MemoryLockingServiceTest
    extends LockingServiceTestCases[String, UUID, Unit]
    with MemoryLockDaoFixtures {
  override def getCurrentLocks(lockDao: LockDaoStub,
                               lockDaoContext: Unit): Set[String] =
    lockDao.asInstanceOf[MemoryLockDao[String, UUID]].locks.keys.toSet
}
