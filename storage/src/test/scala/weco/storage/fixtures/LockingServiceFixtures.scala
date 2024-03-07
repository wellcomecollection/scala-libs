package weco.storage.fixtures

import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues, TryValues}
import weco.Logging
import weco.fixtures.{RandomGenerators, TestWith}
import weco.storage.locking._

import scala.util.Try

trait LockingServiceFixtures[Ident, ContextId, LockDaoContext]
    extends EitherValues
    with TryValues
    with Matchers
    with Logging
    with LockDaoFixtures[Ident, ContextId, LockDaoContext]
    with RandomGenerators {

  type LockDaoStub = LockDao[Ident, ContextId]
  type ResultF = Try[Either[FailedLockingServiceOp, String]]
  type LockingServiceStub =
    LockingService[String, Try, LockDaoStub]

  private def createLockingServiceContextId: ContextId = createContextId

  def withLockingService[R](lockDaoImpl: LockDaoStub)(
    testWith: TestWith[LockingServiceStub, R]): R = {
    val lockingService = new LockingServiceStub {
      type UnlockFail = UnlockFailure[Ident]

      override implicit val lockDao: LockDaoStub = lockDaoImpl
      override protected def createContextId(): ContextId =
        createLockingServiceContextId
    }

    testWith(lockingService)
  }

  def successfulRightOf(result: ResultF): String =
    result.success.value.value

  def successfulLeftOf(result: ResultF): FailedLockingServiceOp =
    result.success.value.left.value

  def assertLockSuccess(result: ResultF): Assertion = {
    debug(s"Got $result")
    successfulRightOf(result) shouldBe expectedResult
  }

  def assertFailedLock(result: ResultF,
                       lockIds: Set[Ident],
                       expectedFailures: Set[Ident]): Assertion = {
    debug(s"Got $result, with $lockIds")
    val failedLock = successfulLeftOf(result)
      .asInstanceOf[FailedLock[ContextId, Ident]]

    failedLock.lockFailures shouldBe a[Set[_]]
    failedLock.lockFailures.map { _.id } shouldBe expectedFailures
  }

  def assertFailedProcess(result: ResultF, e: Throwable): Assertion = {
    val failedLock = successfulLeftOf(result)
      .asInstanceOf[FailedProcess[ContextId]]

    failedLock.e shouldBe e
  }

  val expectedResult: String = randomAlphanumeric()
  val expectedError: Error = new Error(randomAlphanumeric())

  def f = Try { expectedResult }
  def fError = Try { throw expectedError }
}
