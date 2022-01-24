package weco.messaging.worker.steps

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.messaging.fixtures.worker.WorkerFixtures

class MessageTransformTest
    extends AnyFunSpec
    with Matchers
    with WorkerFixtures {
  it("calls transform function and returns result") {
    val messageTransform = new MessageTransform[MyMessage, MyWork] {
      override val transform: MyMessage => Either[Throwable, MyWork] =
        (_: MyMessage) => Right(work)
    }

    messageTransform.callTransform(message) shouldBe Right(work)
  }

  it("returns Left if transform function throws an exception") {
    val exception = new RuntimeException

    val messageTransform = new MessageTransform[MyMessage, MyWork] {
      override val transform: MyMessage => Either[Throwable, MyWork] =
        (_: MyMessage) => throw exception
    }

    messageTransform.callTransform(message) shouldBe Left(exception)
  }
}
