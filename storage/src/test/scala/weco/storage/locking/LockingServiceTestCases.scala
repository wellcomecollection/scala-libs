package weco.storage.locking

import cats.implicits._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.TestWith
import weco.storage.fixtures.LockingServiceFixtures
import weco.storage.locking.memory.PermanentLock
import weco.storage.fixtures.LockingServiceFixtures

import scala.util.{Success, Try}

trait LockingServiceTestCases[Ident, ContextId, LockDaoContext]
    extends AnyFunSpec
    with Matchers
    with LockingServiceFixtures[Ident, ContextId, LockDaoContext] {
  def getCurrentLocks(lockDao: LockDaoStub,
                      lockDaoContext: LockDaoContext): Set[Ident]

  val commonLockIds = Set(createIdent)
  val nonOverlappingLockIds = Set(createIdent, createIdent)

  val lockIds: Set[Ident] = Set(createIdent, createIdent) ++ commonLockIds

  val overlappingLockIds: Set[Ident] = commonLockIds ++ nonOverlappingLockIds
  val differentLockIds = Set(createIdent, createIdent, createIdent)

  def withLockingServiceImpl[R](testWith: TestWith[LockingServiceStub, R]): R =
    withLockDaoContext { lockDaoContext =>
      withLockDao(lockDaoContext) { lockDao =>
        withLockingService(lockDao) { service =>
          testWith(service)
        }
      }
    }

  describe("behaves as a LockingService") {
    it("acquires a lock successfully, and returns the result") {
      withLockDaoContext { lockDaoContext =>
        withLockDao(lockDaoContext) { lockDao =>
          withLockingService(lockDao) { service =>
            assertLockSuccess(service.withLocks(lockIds) {
              getCurrentLocks(lockDao, lockDaoContext) shouldBe lockIds
              f
            })
          }
        }
      }
    }

    it("allows locking a single identifier") {
      val id = createIdent

      withLockDaoContext { lockDaoContext =>
        withLockDao(lockDaoContext) { lockDao =>
          withLockingService(lockDao) { service =>
            assertLockSuccess(service.withLock(id) {
              getCurrentLocks(lockDao, lockDaoContext) shouldBe Set(id)
              f
            })
          }
        }
      }
    }

    it("fails if you try to re-lock the same identifiers twice") {
      withLockDaoContext { lockDaoContext =>
        withLockDao(lockDaoContext) { lockDao =>
          withLockingService(lockDao) { service =>
            assertLockSuccess(service.withLocks(lockIds) {
              assertFailedLock(
                result = service.withLocks(lockIds)(f),
                lockIds = lockIds,
                expectedFailures = lockIds
              )

              // Check the original locks were preserved
              getCurrentLocks(lockDao, lockDaoContext) shouldBe lockIds

              f
            })
          }
        }
      }
    }

    it("fails if you try to re-lock an already locked identifier") {
      withLockDaoContext { lockDaoContext =>
        withLockDao(lockDaoContext) { lockDao =>
          withLockingService(lockDao) { service =>
            assertLockSuccess(service.withLocks(lockIds) {
              assertFailedLock(
                result = service.withLocks(overlappingLockIds)(f),
                lockIds = commonLockIds,
                expectedFailures = overlappingLockIds
              )

              // Check the original locks were preserved
              getCurrentLocks(lockDao, lockDaoContext) shouldBe lockIds

              f
            })
          }
        }
      }
    }

    it("allows multiple, nested locks on different identifiers") {
      withLockingServiceImpl { service =>
        assertLockSuccess(service.withLocks(lockIds) {
          assertLockSuccess(service.withLocks(differentLockIds)(f))

          f
        })
      }
    }

    it("unlocks a context set when done, and allows you to re-lock them") {
      withLockingServiceImpl { service =>
        assertLockSuccess(service.withLocks(lockIds)(f))
        assertLockSuccess(service.withLocks(lockIds)(f))
      }
    }

    it("unlocks a context set when a result throws a Throwable") {
      withLockingServiceImpl { service =>
        assertFailedProcess(service.withLocks(lockIds)(fError), expectedError)
        assertLockSuccess(service.withLocks(lockIds)(f))
      }
    }

    it("unlocks a context set when a partial lock is acquired") {
      withLockingServiceImpl { service =>
        assertLockSuccess(service.withLocks(lockIds) {

          assertFailedLock(
            result = service.withLocks(overlappingLockIds)(f),
            lockIds = commonLockIds,
            expectedFailures = overlappingLockIds
          )

          assertLockSuccess(
            service.withLocks(nonOverlappingLockIds)(f)
          )

          f
        })
      }
    }

    it("calls the callback if asked to lock an empty set") {
      withLockingServiceImpl { service =>
        assertLockSuccess(
          service.withLocks(Set.empty)(f)
        )
      }
    }

    it("returns a success even if unlocking fails") {
      val brokenUnlockDao = new LockDao[Ident, ContextId] {
        override def lock(id: Ident, contextId: ContextId): LockResult =
          Right(PermanentLock(id = id, contextId = contextId))

        override def unlock(contextId: ContextId): UnlockResult =
          Left(UnlockFailure(contextId, new Throwable("BOOM!")))
      }

      withLockingService(brokenUnlockDao) { service =>
        assertLockSuccess(
          service.withLocks(lockIds)(f)
        )
      }
    }

    it("releases locks if the callback fails") {
      withLockDaoContext { lockDaoContext =>
        withLockDao(lockDaoContext) { lockDao =>
          withLockingService(lockDao) { service =>
            val result = service.withLocks(lockIds) {
              Try {
                throw new Throwable("BOOM!")
              }
            }

            result.get.left.value shouldBe a[FailedProcess[_]]
            getCurrentLocks(lockDao, lockDaoContext) shouldBe empty
          }
        }
      }
    }

    // This is mimicing a failure in the bag versioner in the storage service.
    //
    // The bag versioner locks around two IDs: the ingest ID and the external ID.
    // Each process has a different ingest ID, but different processes may have
    // the same external ID.
    //
    // We'd see the following steps:
    //
    //    proc1: lock(ingestId=proc1)              -> success
    //    proc1: lock(externalId=testing/test_bag) -> success
    //    proc2: lock(ingestId=proc2)              -> success
    //    proc2: lock(externalId=testing/test_bag) -> fail
    //
    // and then the lock for `ingestId=proc2` would never be rolled back, which
    // prevented proc2 from proceeding when it was retried.
    //
    it("releases any already-acquired locks if one of the IDs fails to lock") {
      val commonId = createIdent

      val lockSet1 = Set(commonId, createIdent, createIdent)
      val lockSet2 = Set(createIdent, commonId, createIdent)

      println(s"lockSet1 = $lockSet1")
      println(s"lockSet2 = $lockSet2")

      withLockDaoContext { lockDaoContext =>
        withLockDao(lockDaoContext) { lockDao =>
          withLockingService(lockDao) { service =>
            service.withLocks(lockSet1) {
              service.withLocks(lockSet2) {
                Success("OK!")
              }

              Success("OK!")
            }

            getCurrentLocks(lockDao, lockDaoContext) shouldBe empty
          }
        }
      }
    }
  }
}
