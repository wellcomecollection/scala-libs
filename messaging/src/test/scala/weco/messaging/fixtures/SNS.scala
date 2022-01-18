package weco.messaging.fixtures

import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.{
  CreateTopicRequest,
  DeleteTopicRequest,
  SubscribeRequest
}
import weco.fixtures._
import weco.json.JsonUtil._
import weco.messaging.fixtures.SQS.Queue
import weco.messaging.sns.NotificationMessage

import java.net.URI

object SNS {
  case class Topic(arn: String, destinationQueue: Queue) {
    override def toString = s"SNS.Topic($arn)"
  }
}

trait SNS extends SQS {

  import SNS._

  private val localSNSEndpointUrl = "http://localhost:4566"

  implicit val snsClient: SnsClient =
    SnsClient
      .builder()
      .region(Region.of("localhost"))
      .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("access", "key")))
      .endpointOverride(new URI(localSNSEndpointUrl))
      .build()

  def createTopicName: String =
    randomAlphanumeric()

  private def withTopic[R](destinationQueue: Queue): Fixture[Topic, R] =
    fixture[Topic, R](
      create = {
        val topicName = createTopicName

        // Create the SNS topic
        val arn = snsClient
          .createTopic { builder: CreateTopicRequest.Builder =>
            builder.name(topicName)
          }
          .topicArn()

        // Subscribe the SQS queue to the newly-created topic
        snsClient
          .subscribe { builder: SubscribeRequest.Builder =>
            builder
              .topicArn(arn)
              .protocol("sqs")
              .endpoint(destinationQueue.arn)
          }

        Topic(arn, destinationQueue)
      },
      destroy = { topic =>
        snsClient.deleteTopic { builder: DeleteTopicRequest.Builder =>
          builder.topicArn(topic.arn)
        }
      }
    )

  def withLocalSnsTopic[R](testWith: TestWith[Topic, R]): R =
    withLocalSqsQueue() { destinationQueue =>
      withTopic(destinationQueue) { topic =>
        testWith(topic)
      }
    }

  def listMessagesReceivedFromSns(topic: Topic): Seq[String] =
    getMessages(topic.destinationQueue)
      .map(msg => fromJson[NotificationMessage](msg.body()).get)
      .map(_.body)
}
