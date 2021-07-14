package weco.messaging.fixtures

import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import io.circe.Encoder
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model._
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import weco.fixtures._
import weco.json.JsonUtil._
import weco.messaging.sns.NotificationMessage
import weco.messaging.sqs._
import weco.monitoring.Metrics
import weco.monitoring.memory.MemoryMetrics

import java.net.URI
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

trait SQS extends Matchers with Logging with RandomGenerators {

  import SQS._

  private val sqsInternalEndpointUrl = "http://sqs:9324"

  def endpoint(queue: Queue) =
    s"aws-sqs://${queue.name}?amazonSQSEndpoint=$sqsInternalEndpointUrl&accessKey=&secretKey="

  implicit val asyncSqsClient: SqsAsyncClient =
    SqsAsyncClient.builder()
      .region(Region.of("localhost"))
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create("access", "key")))
      .endpointOverride(new URI("http://localhost:9324"))
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

  private def getQueueAttribute(queueUrl: String,
                                attributeName: QueueAttributeName): String =
    asyncSqsClient
      .getQueueAttributes { builder: GetQueueAttributesRequest.Builder =>
        builder
          .queueUrl(queueUrl)
          .attributeNames(attributeName)
      }
      .get()
      .attributes()
      .get(attributeName)

  def getQueueAttribute(
    queue: Queue,
    attributeName: QueueAttributeName): String =
    getQueueAttribute(
      queueUrl = queue.url,
      attributeName = attributeName
    )

  def createQueueName: String =
    randomAlphanumeric()

  def withLocalSqsQueue[R](
    queueName: String = createQueueName,
    visibilityTimeout: Duration = 5.seconds
  ): Fixture[Queue, R] =
    fixture[Queue, R](
      create = {
        val response = asyncSqsClient
          .createQueue {
            builder: CreateQueueRequest.Builder =>
              builder.queueName(queueName)
          }
          .get()

        val arn = getQueueAttribute(
          queueUrl = response.queueUrl(),
          attributeName = QueueAttributeName.QUEUE_ARN
        )

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
        asyncSqsClient
          .purgeQueue { builder: PurgeQueueRequest.Builder =>
            builder.queueUrl(queue.url)
          }
          .get
        asyncSqsClient
          .deleteQueue { builder: DeleteQueueRequest.Builder =>
            builder.queueUrl(queue.url)
          }
          .get
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

  def createNotificationMessageWith(body: String): NotificationMessage =
    NotificationMessage(body = body)

  def createNotificationMessageWith[T](message: T)(
    implicit encoder: Encoder[T]): NotificationMessage =
    createNotificationMessageWith(body = toJson(message).get)

  def sendNotificationToSQS(queue: Queue, body: String): SendMessageResponse = {
    val message = createNotificationMessageWith(body = body)

    sendSqsMessage(queue = queue, obj = message)
  }

  def sendNotificationToSQS[T](queue: Queue, message: T)(
    implicit encoder: Encoder[T]): SendMessageResponse =
    sendNotificationToSQS(queue = queue, body = toJson[T](message).get)

  def sendSqsMessage[T](queue: Queue, obj: T)(
    implicit encoder: Encoder[T]): SendMessageResponse =
    sendMessageToSqsClient(queue = queue, body = toJson[T](obj).get)

  def sendInvalidJSONto(queue: Queue): SendMessageResponse =
    sendMessageToSqsClient(
      queue = queue,
      body = randomAlphanumeric())

  private def sendMessageToSqsClient(queue: Queue,
                                     body: String): SendMessageResponse = {
    debug(s"Sending message to ${queue.url}: $body")

    asyncSqsClient
      .sendMessage { builder: SendMessageRequest.Builder =>
        builder.queueUrl(queue.url).messageBody(body)
      }
      .get
  }

  def noMessagesAreWaitingIn(queue: Queue): Assertion = {
    val messagesInFlight = getQueueAttribute(
      queue,
      attributeName =
        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
    )

    assert(
      messagesInFlight == "0",
      s"Expected messages in flight on ${queue.url} to be 0, actually $messagesInFlight"
    )

    val messagesWaiting = getQueueAttribute(
      queue,
      attributeName = QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES
    )

    assert(
      messagesWaiting == "0",
      s"Expected messages waiting on ${queue.url} to be 0, actually $messagesWaiting"
    )
  }

  def assertQueueEmpty(queue: Queue): Assertion = {
    waitVisibilityTimeoutExpiry(queue)

    val messages = getMessages(queue)

    assert(
      messages.isEmpty,
      s"Expected not to get any messages from ${queue.url}, actually got $messages")

    noMessagesAreWaitingIn(queue)
  }

  def assertQueueHasSize(queue: Queue, size: Int): Assertion = {
    waitVisibilityTimeoutExpiry(queue)

    val messages = getMessages(queue)
    val messagesSize = messages.size

    assert(
      messagesSize == size,
      s"Expected queue ${queue.url} to have size $size, actually had size $messagesSize"
    )
  }

  private def waitVisibilityTimeoutExpiry(queue: Queue): Unit = {
    // Wait slightly longer than the visibility timeout to ensure that messages
    // that fail processing become visible again before asserting.
    val millisecondsToWait = (queue.visibilityTimeout.toMillis * 1.5).toInt
    Thread.sleep(millisecondsToWait)
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
