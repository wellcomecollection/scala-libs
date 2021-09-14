package weco.messaging.worker.steps

import java.time.Instant
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.messaging.fixtures.worker.WorkerFixtures

class MessageTransformTest extends AnyFunSpec with Matchers with WorkerFixtures {
  it("calls transform function and returns result") {
    val now = Instant.now
    val messageTransform = new MessageTransform[MyMessage, MyWork, MyContext] {
      override val transform: MyMessage => Transformed = _ => {
        (Right(work), Right(Some(now)))
      }
    }

    messageTransform.callTransform(message) shouldBe (
      (
        Right(work),
        Right(Some(now))))
  }

  it("returns Left if transform function throws an exception") {
    val exception = new RuntimeException

    val messageTransform = new MessageTransform[MyMessage, MyWork, MyContext] {
      override val transform: MyMessage => Transformed = _ => {
        throw exception
      }
    }

    messageTransform.callTransform(message) shouldBe (
      (
        Left(exception),
        Left(exception)))
  }
}
