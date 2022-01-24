package weco.messaging.sqsworker.alpakka

import org.scalatest.concurrent.{
  AbstractPatienceConfiguration,
  Eventually,
  ScalaFutures
}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import weco.akka.fixtures.Akka
import weco.json.JsonUtil._
import weco.messaging.fixtures.SQS.QueuePair
import weco.messaging.fixtures.monitoring.metrics.MetricsFixtures
import weco.messaging.fixtures.worker.AlpakkaSQSWorkerFixtures

import scala.concurrent.ExecutionContext.Implicits.global

class AlpakkaSQSWorkerTest
    extends AnyFunSpec
    with Matchers
    with AlpakkaSQSWorkerFixtures
    with MetricsFixtures
    with ScalaFutures
    with Eventually
    with AbstractPatienceConfiguration
    with Akka {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = scaled(Span(30, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  val namespace = "AlpakkaSQSWorkerTest"

  describe("When a message is processed") {
    it("consumes a message and increments success metrics") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, successful, namespace) {
              case (worker, _, metrics, callCounter) =>
                worker.start

                val myWork = MyWork("my-new-work")

                sendNotificationToSQS(queue, myWork)

                eventually {
                  callCounter.calledCount shouldBe 1

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

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, 0)
                }
            }
          }
      }
    }

    it("processes lots of messages") {
      val works = (1 to 20).map { i =>
        MyWork(s"my-work-$i")
      }

      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, successful, namespace) {
              case (worker, _, metrics, callCounter) =>
                worker.start

                works.foreach { sendNotificationToSQS(queue, _) }

                eventually {
                  callCounter.calledCount shouldBe works.size

                  assertMetricCount(
                    metrics = metrics,
                    metricName = s"$namespace/Successful",
                    expectedCount = works.size
                  )
                  assertMetricDurations(
                    metrics = metrics,
                    metricName = s"$namespace/Duration",
                    expectedNumberDurations = works.size
                  )

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, 0)
                }
            }
          }
      }
    }

    it("records a failure if it can't process a message, then DLQs the message") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, terminalFailure, namespace) {
              case (worker, _, metrics, callCounter) =>
                worker.start

                val myWork = MyWork("my-new-work")

                sendNotificationToSQS(queue, myWork)

                eventually {
                  callCounter.calledCount shouldBe 1

                  assertMetricCount(
                    metrics = metrics,
                    metricName = s"$namespace/TerminalFailure",
                    expectedCount = 1
                  )
                  assertMetricDurations(
                    metrics = metrics,
                    metricName = s"$namespace/Duration",
                    expectedNumberDurations = 1
                  )

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, size = 1)
                }
            }
          }
      }
    }

    it("retries a retryable failure three times, then DLQs the message") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, retryableFailure, namespace) {
              case (worker, _, metrics, callCounter) =>
                worker.start

                val myWork = MyWork("my-new-work")

                sendNotificationToSQS(queue, myWork)

                eventually {
                  callCounter.calledCount shouldBe 3

                  assertMetricCount(
                    metrics = metrics,
                    metricName = s"$namespace/RetryableFailure",
                    expectedCount = 3
                  )
                  assertMetricDurations(
                    metrics = metrics,
                    metricName = s"$namespace/Duration",
                    expectedNumberDurations = 3
                  )

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, size = 1)
                }
            }
          }
      }
    }
  }

  describe("unparseable messages are recorded and moved to a DLQ") {
    it("if they're not JSON") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, successful, namespace) {
              case (worker, _, metrics, _) =>
                worker.start

                sendNotificationToSQS(queue, "not json")

                eventually {
                  assertMetricCount(
                    metrics = metrics,
                    metricName = s"$namespace/TerminalFailure",
                    expectedCount = 1
                  )
                  assertMetricDurations(
                    metrics = metrics,
                    metricName = s"$namespace/Duration",
                    expectedNumberDurations = 1
                  )

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, size = 1)
                }
            }
          }
      }
    }

    it("if they can't be parsed") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, successful, namespace) {
              case (worker, _, metrics, _) =>
                worker.start

                sendNotificationToSQS(queue, """{"json" : "but not the right format"}""")

                eventually {
                  assertMetricCount(
                    metrics = metrics,
                    metricName = s"$namespace/TerminalFailure",
                    expectedCount = 1
                  )
                  assertMetricDurations(
                    metrics = metrics,
                    metricName = s"$namespace/Duration",
                    expectedNumberDurations = 1
                  )

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, size = 1)
                }
            }
          }
      }
    }
  }
}
