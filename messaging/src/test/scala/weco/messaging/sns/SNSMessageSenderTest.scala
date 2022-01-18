package weco.messaging.sns

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sns.model.SnsException
import weco.messaging.fixtures.SNS

import scala.util.{Failure, Success}

class SNSMessageSenderTest extends AnyFunSpec with Matchers with SNS with Eventually with IntegrationPatience {
  it("sends messages to SNS") {
    withLocalSnsTopic { topic =>
      val sender = new SNSIndividualMessageSender(snsClient)

      sender.send("hello world")(
        subject = "Sent from SNSMessageSenderTest",
        destination = SNSConfig(topic.arn)
      ) shouldBe Success(())

      eventually {
        listMessagesReceivedFromSns(topic) shouldBe Seq("hello world")
      }
    }
  }

  it("fails if it cannot send to SNS") {
    val sender = new SNSIndividualMessageSender(snsClient)

    val result = sender.send("hello world")(
      subject = "Sent from SNSMessageSenderTest",
      destination = SNSConfig(topicArn = "arn::doesnotexist")
    )

    result shouldBe a[Failure[_]]
    val err = result.failed.get
    err shouldBe a[SnsException]
    err.getMessage should startWith("Topic does not exist")
  }
}
