package weco.messaging.fixtures

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import io.circe.Encoder
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model._
import weco.fixtures._
import weco.json.JsonUtil.toJson
import weco.messaging.sns.NotificationMessage
import weco.messaging.sqs._
import weco.monitoring.Metrics
import weco.monitoring.memory.MemoryMetrics

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

object SQS {
  case class Queue(url: String, arn: String, visibilityTimeout: Duration) {
    override def toString = s"SQS.Queue(url = $url, name = $name)"
    def name: String = url.split("/").toList.last
  }
  case class QueuePair(queue: Queue, dlq: Queue)
}

trait SQS extends Matchers with Logging with RandomGenerators with LocalStackFixtures {

  import SQS._

  private val sqsInternalEndpointUrl = s"http://sqs:$port"

  def endpoint(queue: Queue) =
    s"aws-sqs://${queue.name}?amazonSQSEndpoint=$sqsInternalEndpointUrl&accessKey=&secretKey="

  implicit val asyncSqsClient: SqsAsyncClient =
    SqsAsyncClient
      .builder()
      .region(region)
      .credentialsProvider(credentials)
      .endpointOverride(endpoint)
      .build()

  private def setQueueAttribute(
    queue: Queue,
    attributeName: QueueAttributeName,
    attributeValue: String): SetQueueAttributesResponse =
    asyncSqsClient
      .setQueueAttributes { builder: SetQueueAttributesRequest.Builder =>
        builder
          .queueUrl(queue.url)
          .attributes(Map(attributeName -> attributeValue).asJava)
      }
      .get()

  private def getQueueAttributes(
    queueUrl: String,
    attributeNames: QueueAttributeName*): Map[QueueAttributeName, String] = {
    val attributeValues =
      asyncSqsClient
        .getQueueAttributes { builder: GetQueueAttributesRequest.Builder =>
          builder
            .queueUrl(queueUrl)
            .attributeNames(attributeNames: _*)
        }
        .get()
        .attributes()

    attributeNames.map { name =>
      name -> attributeValues.get(name)
    }.toMap
  }

  def createQueueName: String =
    randomAlphanumeric()

  def withLocalSqsQueue[R](
    queueName: String = createQueueName,
    visibilityTimeout: Duration = 5.seconds
  ): Fixture[Queue, R] =
    fixture[Queue, R](
      create = {
        val response = asyncSqsClient
          .createQueue { builder: CreateQueueRequest.Builder =>
            builder.queueName(queueName)
          }
          .get()

        val arn =
          getQueueAttributes(response.queueUrl(), QueueAttributeName.QUEUE_ARN)(
            QueueAttributeName.QUEUE_ARN)

        val queue = Queue(
          url = response.queueUrl(),
          arn = arn,
          visibilityTimeout = visibilityTimeout
        )

        setQueueAttribute(
          queue = queue,
          attributeName = QueueAttributeName.VISIBILITY_TIMEOUT,
          attributeValue = visibilityTimeout.toSeconds.toString
        )

        queue
      },
      destroy = { queue =>
        asyncSqsClient.purgeQueue { builder: PurgeQueueRequest.Builder =>
          builder.queueUrl(queue.url)
        }.get
        asyncSqsClient.deleteQueue { builder: DeleteQueueRequest.Builder =>
          builder.queueUrl(queue.url)
        }.get
      }
    )

  def withLocalSqsQueuePair[R](visibilityTimeout: Duration = 5.seconds)(
    testWith: TestWith[QueuePair, R]): R = {
    val queueName = createQueueName

    withLocalSqsQueue(queueName = s"$queueName-dlq") { dlq =>
      withLocalSqsQueue(
        queueName = queueName,
        visibilityTimeout = visibilityTimeout) { queue =>
        setQueueAttribute(
          queue = queue,
          attributeName = QueueAttributeName.REDRIVE_POLICY,
          attributeValue =
            s"""{"maxReceiveCount":"3", "deadLetterTargetArn":"${dlq.arn}"}"""
        )

        testWith(QueuePair(queue, dlq))
      }
    }
  }

  def withSQSStream[T, R](
    queue: Queue,
    metrics: Metrics[Future] = new MemoryMetrics()
  )(testWith: TestWith[SQSStream[T], R])(
    implicit actorSystem: ActorSystem): R = {
    val sqsConfig = createSQSConfigWith(queue)

    val stream = new SQSStream[T](
      sqsClient = asyncSqsClient,
      sqsConfig = sqsConfig,
      metricsSender = metrics
    )

    testWith(stream)
  }

  def createNotificationMessageWith[T](message: T)(
    implicit encoder: Encoder[T]): NotificationMessage =
    NotificationMessage(body = toJson(message).get)

  def sendNotificationToSQS(queue: Queue, body: String): SendMessageResponse = {
    val message = NotificationMessage(body = body)

    sendSqsMessage(queue = queue, obj = message)
  }

  def sendNotificationToSQS[T](queue: Queue, message: T)(
    implicit encoder: Encoder[T]): SendMessageResponse =
    sendNotificationToSQS(queue = queue, body = toJson[T](message).get)

  def sendSqsMessage[T](queue: Queue, obj: T)(
    implicit encoder: Encoder[T]): SendMessageResponse =
    sendMessageToSqsClient(queue = queue, body = toJson[T](obj).get)

  def sendInvalidJSONto(queue: Queue): SendMessageResponse =
    sendMessageToSqsClient(queue = queue, body = randomAlphanumeric())

  private def sendMessageToSqsClient(queue: Queue,
                                     body: String): SendMessageResponse = {
    debug(s"Sending message to ${queue.url}: $body")

    asyncSqsClient.sendMessage { builder: SendMessageRequest.Builder =>
      builder.queueUrl(queue.url).messageBody(body)
    }.get
  }

  /** Returns a rough count of all the messages on a queue. */
  private def countMessagesOnQueue(
    queue: Queue): Map[QueueAttributeName, Int] = {
    val attributeNames = Seq(
      // Messages available for retrieval
      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
      // Messages in the queue and not available for reading immediately
      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
      // Messages currently in flight
      QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
    )

    getQueueAttributes(queue.url, attributeNames: _*)
      .map { case (name, count) => name -> count.toInt }
  }

  def assertQueueEmpty(queue: Queue): Unit = {
    countMessagesOnQueue(queue).foreach {
      case (name, count) =>
        assert(
          count == 0,
          s"Expected ${queue.url} to have $name == 0, got $name == $count"
        )
    }
  }

  def assertQueueHasSize(queue: Queue, size: Int): Assertion = {
    val counts = countMessagesOnQueue(queue)

    assert(
      counts(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES) == size,
      s"Expected queue ${queue.url} to have $size messages available, actually has ${counts(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)}"
    )

    assert(
      counts(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED) == 0,
      s"Expected queue ${queue.url} to have $size messages delayed, actually has ${counts(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)}"
    )

    assert(
      counts(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE) == 0,
      s"Expected queue ${queue.url} to have $size messages in flight, actually has ${counts(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)}"
    )
  }

  def getMessages(queue: Queue): Seq[Message] =
    asyncSqsClient
      .receiveMessage { builder: ReceiveMessageRequest.Builder =>
        builder.queueUrl(queue.url).maxNumberOfMessages(10)
      }
      .get
      .messages()
      .asScala

  def createSQSConfigWith(queue: Queue): SQSConfig =
    SQSConfig(queueUrl = queue.url)
}
