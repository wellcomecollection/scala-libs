package weco.messaging.worker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.messaging.fixtures.monitoring.metrics.MetricsFixtures
import weco.messaging.fixtures.worker.WorkerFixtures
import weco.monitoring.memory.MemoryMetrics

import scala.concurrent.ExecutionContext.Implicits.global

class WorkerTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with WorkerFixtures
    with MetricsFixtures {

  it("successfully processes a work and increments success metrics") {
    val namespace = s"ns-${randomAlphanumeric()}"
    implicit val metrics = new MemoryMetrics()

    val worker = new MyWorker(
      namespace = namespace,
      testProcess = successful,
      transform = messageToWork(shouldFail = false)
    )

    val process = worker.processMessage(message)
    whenReady(process) { _ =>
      worker.callCounter.calledCount shouldBe 1

      assertMetricCount(
        metrics = metrics,
        metricName = s"$namespace/Successful",
        expectedCount = 1
      )

      assertMetricDurations(
        metrics = metrics,
        metricName = s"$namespace/Duration",
        expectedNumberDurations = 1
      )
    }
  }

  it("increments deterministic failure metric if transformation returns a Left") {
    val namespace = s"ns-${randomAlphanumeric()}"
    implicit val metrics = new MemoryMetrics()

    val worker = new MyWorker(
      namespace = namespace,
      testProcess = successful,
      transform = messageToWork(shouldFail = true)
    )

    val process = worker.processMessage(message)
    whenReady(process) { _ =>
      worker.callCounter.calledCount shouldBe 0

      assertMetricCount(
        metrics = metrics,
        metricName = s"$namespace/DeterministicFailure",
        expectedCount = 1
      )

      assertMetricDurations(
        metrics = metrics,
        metricName = s"$namespace/Duration",
        expectedNumberDurations = 1
      )
    }
  }

  it(
    "increments deterministic failure metric if transformation fails unexpectedly") {
    def transform(message: MyMessage) = throw new RuntimeException

    val namespace = s"ns-${randomAlphanumeric()}"
    implicit val metrics = new MemoryMetrics()

    val worker = new MyWorker(
      namespace = namespace,
      testProcess = successful,
      transform = transform
    )

    val process = worker.processMessage(message)
    whenReady(process) { _ =>
      worker.callCounter.calledCount shouldBe 0

      assertMetricCount(
        metrics = metrics,
        metricName = s"$namespace/DeterministicFailure",
        expectedCount = 1
      )

      assertMetricDurations(
        metrics = metrics,
        metricName = s"$namespace/Duration",
        expectedNumberDurations = 1
      )
    }
  }

  it("doesn't increment metrics if monitoring fails") {
    val namespace = s"ns-${randomAlphanumeric()}"
    implicit val metrics = brokenMemoryMetrics

    val worker = new MyWorker(
      namespace = namespace,
      testProcess = successful,
      transform = messageToWork(shouldFail = false)
    )

    val process = worker.processMessage(message)

    whenReady(process) { _ =>
      worker.callCounter.calledCount shouldBe 1

      metrics.incrementedCounts shouldBe empty

      metrics.recordedValues shouldBe empty
    }
  }

  it(
    "increments deterministic failure metric if processing fails with deterministic failure") {
    val namespace = s"ns-${randomAlphanumeric()}"
    implicit val metrics = new MemoryMetrics()

    val worker = new MyWorker(
      namespace = namespace,
      testProcess = deterministicFailure,
      transform = messageToWork(shouldFail = false)
    )

    val process = worker.processMessage(message)
    whenReady(process) { _ =>
      worker.callCounter.calledCount shouldBe 1

      assertMetricCount(
        metrics = metrics,
        metricName = s"$namespace/DeterministicFailure",
        expectedCount = 1
      )

      assertMetricDurations(
        metrics = metrics,
        metricName = s"$namespace/Duration",
        expectedNumberDurations = 1
      )
    }
  }

  it(
    "increments non deterministic failure metric if processing fails with non deterministic failure") {
    val namespace = s"ns-${randomAlphanumeric()}"
    implicit val metrics = new MemoryMetrics()

    val worker = new MyWorker(
      namespace = namespace,
      testProcess = nonDeterministicFailure,
      transform = messageToWork(shouldFail = false)
    )

    val process = worker.processMessage(message)
    whenReady(process) { _ =>
      worker.callCounter.calledCount shouldBe 1

      assertMetricCount(
        metrics = metrics,
        metricName = s"$namespace/NonDeterministicFailure",
        expectedCount = 1
      )

      assertMetricDurations(
        metrics = metrics,
        metricName = s"$namespace/Duration",
        expectedNumberDurations = 1
      )
    }
  }
}
