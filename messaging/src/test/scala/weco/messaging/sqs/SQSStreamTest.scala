package weco.messaging.sqs

import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.pekko.stream.scaladsl.{Flow, Keep}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.model.Message
import weco.akka.fixtures.Akka
import weco.fixtures.TestWith
import weco.json.JsonUtil._
import weco.messaging.fixtures.SQS
import weco.messaging.fixtures.SQS.{Queue, QueuePair}
import weco.monitoring.memory.MemoryMetrics

import scala.concurrent.duration._
import scala.concurrent.Future

class SQSStreamTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with Eventually
    with SQS
    with Akka {

  case class NamedObject(name: String)

  def process(list: ConcurrentLinkedQueue[NamedObject])(
    o: NamedObject): Future[Unit] = {
    list.add(o)
    Future.successful(())
  }

  it("reads messages off a queue, processes them and deletes them") {
    val messages = createNamedObjects(count = 20)

    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), _) =>
        messages.foreach { sendSqsMessage(queue, _) }

        val received = new ConcurrentLinkedQueue[NamedObject]()
        val streamName = randomAlphanumeric(10)
        messageStream.foreach(
          streamName = streamName,
          process = process(received))

        eventually {
          received should contain theSameElementsAs messages

          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
        }
    }
  }

  it("increments *_ProcessMessage metric when successful") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, _), metricsSender) =>
        sendNamedObjects(queue = queue)

        val received = new ConcurrentLinkedQueue[NamedObject]()
        val streamName = randomAlphanumeric(10)
        messageStream.foreach(
          streamName = streamName,
          process = process(received))

        eventually {
          metricsSender.incrementedCounts shouldBe Seq(
            s"${streamName}_ProcessMessage_success")
        }
    }
  }

  it("fails gracefully when the object cannot be deserialised") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), metricsSender) =>
        sendInvalidJSONto(queue)

        val received = new ConcurrentLinkedQueue[NamedObject]()
        val streamName = randomAlphanumeric(10)
        messageStream.foreach(
          streamName = streamName,
          process = process(received))

        eventually {
          metricsSender.incrementedCounts shouldBe Seq(
            s"${streamName}_ProcessMessage_jsonDecodingFailure",
            s"${streamName}_ProcessMessage_jsonDecodingFailure",
            s"${streamName}_ProcessMessage_jsonDecodingFailure"
          )
          received shouldBe empty

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it(
    "sends a failure metric if it doesn't fail gracefully when processing a message") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), metricsSender) =>
        val exampleObject = NamedObject("some value 1")

        sendSqsMessage(queue, exampleObject)

        def processFailing(o: NamedObject) = {
          Future.failed(new RuntimeException("BOOOOM!"))
        }
        val streamName = randomAlphanumeric(10)

        messageStream.foreach(streamName = streamName, process = processFailing)

        eventually {
          metricsSender.incrementedCounts shouldBe Seq(
            s"${streamName}_ProcessMessage_failure",
            s"${streamName}_ProcessMessage_failure",
            s"${streamName}_ProcessMessage_failure"
          )
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)
        }
    }
  }

  it("continues reading if processing of some messages fails") {
    withSQSStreamFixtures {
      case (messageStream, QueuePair(queue, dlq), _) =>
        sendNamedObjects(queue = queue, start = 1)
        sendInvalidJSONto(queue)

        sendNamedObjects(queue = queue, start = 2)
        sendInvalidJSONto(queue)

        val received = new ConcurrentLinkedQueue[NamedObject]()
        val streamName = randomAlphanumeric(10)

        messageStream.foreach(
          streamName = streamName,
          process = process(received))

        eventually {
          received should contain theSameElementsAs createNamedObjects(
            count = 2)

          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 2)
        }
    }
  }

  describe("runStream") {
    it("processes messages off a queue ") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), metricsSender) =>
          sendNamedObjects(queue = queue, start = 1, count = 2)

          val received = new ConcurrentLinkedQueue[NamedObject]()
          val streamName = randomAlphanumeric(10)

          messageStream.runStream(
            streamName,
            source =>
              source.via(Flow.fromFunction {
                case (message, t) =>
                  received.add(t)
                  message
              }))

          eventually {
            received should contain theSameElementsAs createNamedObjects(
              count = 2)

            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)

            metricsSender.incrementedCounts shouldBe Seq(
              s"${streamName}_ProcessMessage_success",
              s"${streamName}_ProcessMessage_success"
            )
          }
      }
    }

    it("does not delete failed messages and sends a failure metric") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), metricsSender) =>
          sendNamedObjects(queue = queue)
          val streamName = randomAlphanumeric(10)

          messageStream.runStream(
            streamName,
            source =>
              source.via(
                Flow.fromFunction(_ => throw new RuntimeException("BOOOM!"))))

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, 1)

            metricsSender.incrementedCounts shouldBe Seq(
              s"${streamName}_ProcessMessage_failure",
              s"${streamName}_ProcessMessage_failure",
              s"${streamName}_ProcessMessage_failure"
            )
          }
      }
    }

    it("continues reading if processing of some messages fails") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), _) =>
          sendNamedObjects(queue = queue, start = 1)
          sendInvalidJSONto(queue)

          sendNamedObjects(queue = queue, start = 2)
          sendInvalidJSONto(queue)

          val received = new ConcurrentLinkedQueue[NamedObject]()
          messageStream.runStream(
            randomAlphanumeric(10),
            source =>
              source.via(Flow.fromFunction {
                case (message, t) =>
                  received.add(t)
                  message
              }))

          eventually {
            received should contain theSameElementsAs createNamedObjects(
              count = 2)

            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 2)
          }
      }
    }
  }

  describe("runGraph") {
    it("processes messages when a graph is non-linear") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), metricsSender) =>
          sendNamedObjects(queue = queue, start = 1, count = 5)

          val received = new ConcurrentLinkedQueue[NamedObject]()
          val streamName = randomAlphanumeric(10)

          messageStream.runGraph(streamName) { (source, sink) =>
            source.zipWithIndex
              .divertTo(
                that = sink.contramap[((Message, NamedObject), Long)] {
                  case ((msg, namedObject), _) =>
                    received.add(
                      namedObject.copy(name = "Diverted " + namedObject.name))
                    msg
                },
                when = {
                  case (_, i) => i >= 3
                }
              )
              .map {
                case ((msg, namedObject), _) =>
                  received.add(namedObject)
                  msg
              }
              .toMat(sink)(Keep.right)
          }
          eventually {
            received should contain theSameElementsAs createNamedObjects(
              count = 5).zipWithIndex.map {
              case (obj, i) if i >= 3 => obj.copy(name = "Diverted " + obj.name)
              case (obj, _)           => obj
            }

            assertQueueEmpty(queue)
            assertQueueEmpty(dlq)

            metricsSender.incrementedCounts shouldBe Seq.fill(5)(
              s"${streamName}_ProcessMessage_success",
            )
          }
      }
    }

    it("handles failures when a graph is non-linear") {
      withSQSStreamFixtures {
        case (messageStream, QueuePair(queue, dlq), metricsSender) =>
          sendNamedObjects(queue = queue, start = 0, count = 5)

          val received = new ConcurrentLinkedQueue[NamedObject]()
          val streamName = randomAlphanumeric(10)

          messageStream.runGraph(streamName) { (source, sink) =>
            source.zipWithIndex
              .divertTo(
                sink.contramap[((Message, NamedObject), Long)] { _ =>
                  throw new RuntimeException("oops")
                }, {
                  case (_, i) => i >= 3
                }
              )
              .map {
                case ((msg, namedObject), _) =>
                  received.add(namedObject)
                  msg
              }
              .toMat(sink)(Keep.right)
          }
          eventually {
            received should contain theSameElementsAs
              createNamedObjects(start = 0, count = 3)

            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 2)

            metricsSender.incrementedCounts shouldBe
              Seq.fill(3)(s"${streamName}_ProcessMessage_success") ++
                Seq.fill(3 * 2)(s"${streamName}_ProcessMessage_failure")
          }
      }
    }
  }

  private def createNamedObjects(start: Int = 1,
                                 count: Int): List[NamedObject] =
    (start until start + count).map { i =>
      NamedObject(s"Example value $i")
    }.toList

  private def sendNamedObjects(queue: Queue, start: Int = 1, count: Int = 1) =
    createNamedObjects(start = start, count = count).map { exampleObject =>
      sendSqsMessage(queue = queue, obj = exampleObject)
    }

  def withSQSStreamFixtures[R](
    testWith: TestWith[(SQSStream[NamedObject], QueuePair, MemoryMetrics), R])
    : R =
    withActorSystem { implicit actorSystem =>
      withLocalSqsQueuePair(visibilityTimeout = 2.seconds) {
        case queuePair @ QueuePair(queue, _) =>
          val metrics = new MemoryMetrics()
          withSQSStream[NamedObject, R](queue, metrics) { stream =>
            testWith((stream, queuePair, metrics))
          }
      }
    }
}
