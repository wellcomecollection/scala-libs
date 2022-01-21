package weco.messaging.sns

import org.scalatest.EitherValues
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.errors.RetryableError
import weco.messaging.MessageSenderError
import weco.messaging.fixtures.SNS

import java.net.URI

class SNSMessageSenderTest
    extends AnyFunSpec
    with Matchers
    with SNS
    with EitherValues
    with Eventually
    with IntegrationPatience {
  it("sends messages to SNS") {
    withLocalSnsTopic { topic =>
      val sender = new SNSIndividualMessageSender(snsClient)

      sender.send("hello world")(
        subject = "Sent from SNSMessageSenderTest",
        destination = SNSConfig(topic.arn)
      ) shouldBe Right(())

      eventually {
        listMessagesReceivedFromSns(topic) shouldBe Seq("hello world")
      }
    }
  }

  it("fails if the topic doesn't exist") {
    val sender = new SNSIndividualMessageSender(snsClient)

    val result = sender.send("hello world")(
      subject = "Sent from SNSMessageSenderTest",
      destination = SNSConfig(topicArn = "arn::doesnotexist")
    )

    result.left.value shouldBe a[MessageSenderError.DestinationDoesNotExist]
  }

  it("fails with a retryable error if it can't connect to SNS") {
    val wrongPortClient = createClientWithEndpoint(new URI(s"http://localhost:${localStackPort + 1}"))

    val sender = new SNSIndividualMessageSender(wrongPortClient)

    val result = sender.send("hello world")(
      subject = "Sent from SNSMessageSenderTest",
      destination = SNSConfig(topicArn = "arn::doesnotexist")
    )

    val err = result.left.value
    err shouldBe a[MessageSenderError.UnknownError]
    err shouldBe a[RetryableError]
  }

  it("fails with a retryable error if it can't resolve the SNS endpoint") {
    val unresolvableClient = createClientWithEndpoint(new URI(s"http://this-cannot-be-resolved.nope"))

    val sender = new SNSIndividualMessageSender(unresolvableClient)

    val result = sender.send("hello world")(
      subject = "Sent from SNSMessageSenderTest",
      destination = SNSConfig(topicArn = "arn::doesnotexist")
    )

    val err = result.left.value
    err shouldBe a[MessageSenderError.UnknownError]
    err shouldBe a[RetryableError]
  }
}
