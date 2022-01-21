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

class AlpakkaSQSProcessorTest
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

    it(
      "consumes a message and increments non deterministic failure metrics metrics") {
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
                    metricName = s"$namespace/DeterministicFailure",
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

    it(
      "retries nonDeterministicFailure 3 times and places the message in the dlq") {
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
                    metricName = s"$namespace/NonDeterministicFailure",
                    expectedCount = 3
                  )
                  assertMetricDurations(
                    metrics = metrics,
                    metricName = s"$namespace/Duration",
                    expectedNumberDurations = 3
                  )

                  assertQueueEmpty(queue)
                  assertQueueHasSize(dlq, 1)
                }
            }
          }
      }
    }
  }

  describe("When a message cannot be parsed") {
    it(
      "consumes the message increments failure metrics if the message is not json") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, successful, namespace) {
              case (worker, _, metrics, _) =>
                worker.start

                sendNotificationToSQS(queue, "not json")

                eventually {
                  //process.called shouldBe false

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

                  assertQueueEmpty(queue)
                  assertQueueEmpty(dlq)
                }

            }
          }
      }
    }

    it(
      "consumes the message increments failure metrics if the message is json but not a work") {
      withLocalSqsQueuePair() {
        case QueuePair(queue, dlq) =>
          withActorSystem { implicit actorSystem =>
            withAlpakkaSQSWorker(queue, successful, namespace) {
              case (worker, _, metrics, _) =>
                worker.start

                sendNotificationToSQS(queue, """{"json" : "but not a work"}""")

                eventually {
                  //process.called shouldBe false

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

                  assertQueueEmpty(queue)
                  assertQueueEmpty(dlq)
                }

            }
          }
      }
    }
  }
}
