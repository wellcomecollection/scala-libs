package weco.storage.locking.memory

import java.util.UUID

import weco.storage.locking.LockDaoTestCases

class MemoryLockDaoTest
    extends LockDaoTestCases[String, UUID, Unit]
    with MemoryLockDaoFixtures
