package weco.messaging.worker

import org.scalatest.Assertion
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.akka.fixtures.Akka
import weco.messaging.fixtures.monitoring.metrics.MetricsFixtures
import weco.messaging.fixtures.worker.WorkerFixtures

import scala.concurrent.ExecutionContext.Implicits.global

class WorkerTest
    extends AnyFunSpec
    with Matchers
    with Akka
    with ScalaFutures
    with WorkerFixtures
    with MetricsFixtures {

  it("successfully processes a work and increments success metrics") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, monitoringProcessor) =>
        val worker = new MyWorker(
          monitoringProcessor,
          successful,
          messageToWork(shouldFail = false)
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
  }

  it("increments deterministic failure metric if transformation returns a Left") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, monitoringProcessor) =>
        val worker = new MyWorker(
          monitoringProcessor,
          successful,
          messageToWork(shouldFail = true)
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
  }

  it(
    "increments deterministic failure metric if transformation fails unexpectedly") {
    def transform(message: MyMessage) = throw new RuntimeException("BOOM!")

    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, monitoringProcessor) =>
        val worker = new MyWorker(
          monitoringProcessor,
          successful,
          transform
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
  }

  it("doesn't increment metrics if monitoring fails") {
    withMetricsMonitoringProcessor[MyWork, Assertion](
      metrics = brokenMemoryMetrics) {
      case (_, metrics, monitoringProcessor) =>
        val worker = new MyWorker(
          monitoringProcessor,
          successful,
          messageToWork(shouldFail = false)
        )

        val process = worker.processMessage(message)

        whenReady(process) { _ =>
          worker.callCounter.calledCount shouldBe 1

          metrics.incrementedCounts shouldBe empty

          metrics.recordedValues shouldBe empty
        }
    }
  }

  it(
    "increments deterministic failure metric if processing fails with deterministic failure") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, monitoringProcessor) =>
        val worker = new MyWorker(
          monitoringProcessor,
          terminalFailure,
          messageToWork(shouldFail = false)
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
  }

  it(
    "increments non deterministic failure metric if processing fails with non deterministic failure") {
    withMetricsMonitoringProcessor[MyWork, Unit]() {
      case (namespace, metrics, monitoringProcessor) =>
        val worker = new MyWorker(
          monitoringProcessor,
          retryableFailure,
          messageToWork(shouldFail = false)
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

}
