package weco.messaging.fixtures

import grizzled.slf4j.Logging
import io.circe.{Decoder, Json, ParsingFailure, yaml}
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.{
  CreateTopicRequest,
  DeleteTopicRequest
}
import weco.fixtures._
import weco.json.JsonUtil._
import weco.messaging.sns.SNSConfig

import java.net.URI
import scala.collection.immutable.Seq

object SNS {
  class Topic(val arn: String) extends AnyVal {
    override def toString = s"SNS.Topic($arn)"
  }

  object Topic {
    def apply(arn: String): Topic = new Topic(arn)
  }
}

trait SNS extends Matchers with Logging with RandomGenerators {

  import SNS._

  private val localSNSEndpointUrl = "http://localhost:9292"

  implicit val snsClient: SnsClient =
    SnsClient.builder()
      .region(Region.of("localhost"))
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create("access", "secret")))
      .endpointOverride(new URI(localSNSEndpointUrl))
      .build()

  def createTopicName: String =
    randomAlphanumeric()

  def withLocalSnsTopic[R]: Fixture[Topic, R] = fixture[Topic, R](
    create = {
      val topicName = createTopicName
      val arn = snsClient
        .createTopic { builder: CreateTopicRequest.Builder =>
          builder.name(topicName)
        }
        .topicArn()
      Topic(arn)
    },
    destroy = { topic =>
      snsClient.deleteTopic { builder: DeleteTopicRequest.Builder =>
        builder.topicArn(topic.arn)
      }
    }
  )

  // For some reason, Circe struggles to decode MessageInfo if you use @JsonKey
  // to annotate the fields, and I don't care enough to work out why right now.
  implicit val messageInfoDecoder: Decoder[MessageInfo] =
    Decoder.forProduct4(":id", ":message", ":subject", ":topic_arn")(
      MessageInfo.apply)

  def listMessagesReceivedFromSNS(topic: Topic): Seq[MessageInfo] = {
    /*
    This is a sample returned by the fake-sns implementation:
    ---
    topics:
    - arn: arn:aws:sns:us-east-1:123456789012:es_ingest
      name: es_ingest
    - arn: arn:aws:sns:us-east-1:123456789012:id_minter
      name: id_minter
    messages:
    - :id: acbca1e1-e3c5-4c74-86af-06a9418e8fe4
      :subject: Foo
      :message: '{"identifiers":[{"source":"Miro","sourceId":"MiroID","value":"1234"}],"title":"some
        image title","accessStatus":null}'
      :topic_arn: arn:aws:sns:us-east-1:123456789012:id_minter
      :structure:
      :target_arn:
      :received_at: 2017-04-18 13:20:45.289912607 +00:00
     */
    val string = scala.io.Source.fromURL(localSNSEndpointUrl).mkString

    val json: Either[ParsingFailure, Json] = yaml.parser.parse(string)

    val snsResponse: SNSResponse = json.right.get
      .as[SNSResponse]
      .right
      .get

    snsResponse.messages
      .filter { _.topicArn == topic.arn }
  }

  def createSNSConfigWith(topic: Topic): SNSConfig =
    SNSConfig(topicArn = topic.arn)
}

case class SNSResponse(
  topics: List[TopicInfo],
  messages: List[MessageInfo] = Nil
)

case class TopicInfo(
  arn: String,
  name: String
)

case class MessageInfo(
  messageId: String,
  message: String,
  subject: String,
  topicArn: String
)
