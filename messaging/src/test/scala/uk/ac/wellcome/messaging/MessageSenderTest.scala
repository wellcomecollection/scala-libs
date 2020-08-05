package uk.ac.wellcome.messaging

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.messaging.memory.{MemoryIndividualMessageSender, MemoryMessageSender}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Success, Try}

class MessageSenderTest extends AnyFunSpec with Matchers with JsonAssertions with ScalaFutures with RandomGenerators {
  it("sends individual messages") {
    val sender = new MemoryIndividualMessageSender()

    sender.send("hello world")(
      subject = "my first message",
      destination = "greetings") shouldBe Success(())
    sender.send("guten tag")(subject = "auf deutsch", destination = "greetings") shouldBe Success(
      ())
    sender.send("你好")(subject = "中文", destination = "greetings") shouldBe Success(
      ())
    sender.send("chinese")(
      subject = "a non-alphabet language",
      destination = "languages") shouldBe Success(())

    sender.messages shouldBe List(
      sender.MemoryMessage("hello world", "my first message", "greetings"),
      sender.MemoryMessage("guten tag", "auf deutsch", "greetings"),
      sender.MemoryMessage("你好", "中文", "greetings"),
      sender.MemoryMessage("chinese", "a non-alphabet language", "languages")
    )
  }

  it("can send many messages in parallel") {
    val sender = new MemoryIndividualMessageSender()

    def send(body: String, subject: String, destination: String): Future[Try[Unit]] =
      Future(sender.send(body)(subject, destination))

    val toSend = Function.tupled(send _)
    val messageCount = randomInt(from = 50, to = 150)

    val messages = (1 to messageCount).map(i =>
      (f"$i-${randomAlphanumeric()}", randomAlphanumeric(), randomAlphanumeric()))

    val eventuallyResults = Future.sequence(messages.map(toSend))
    val expectedResults = messages.map(
      Function.tupled(sender.MemoryMessage.apply)
    ).toSet

    whenReady(eventuallyResults) { results =>
      sender.messages.size shouldBe messageCount
      results.foreach(_ shouldBe Success(()))
      sender.messages.toSet shouldBe expectedResults
    }
  }

  it("encodes case classes as JSON") {
    case class Animal(name: String, legs: Int)

    val sender = new MemoryIndividualMessageSender()

    val dog = Animal(name = "dog", legs = 4)
    val octopus = Animal(name = "octopus", legs = 8)
    val snake = Animal(name = "snake", legs = 0)

    Seq(dog, octopus, snake).map { animal =>
      sender.sendT(animal)(subject = "animals", destination = "all creatures") shouldBe Success(
        ())
    }

    Seq(dog, octopus, snake).zip(sender.messages).map {
      case (animal, message) =>
        assertJsonStringsAreEqual(toJson(animal).get, message.body)
    }
  }

  sealed trait Container {}

  case class Box(sides: Int) extends Container
  case class Bottle(height: Int) extends Container

  val containers = Seq(Box(sides = 3), Box(sides = 4), Bottle(height = 5))

  it("encodes case classes using the type parameter") {
    val sender = new MemoryIndividualMessageSender()

    containers.map { c =>
      sender.sendT[Container](c)(
        destination = "containers",
        subject = "stuff to store things in") shouldBe Success(())
    }

    containers.zip(sender.messages).map {
      case (container, message) =>
        fromJson[Container](message.body).get shouldBe container
    }
  }

  it("sends messages to a default destination/subject") {
    val sender = new MemoryMessageSender() {
      override val destination = "colours"
      override val subject = "ideas for my design"
    }

    sender.send("red") shouldBe Success(())
    sender.send("yellow") shouldBe Success(())
    sender.send("green") shouldBe Success(())
    sender.send("blue") shouldBe Success(())

    sender.messages.map { _.destination } shouldBe Seq(
      "colours",
      "colours",
      "colours",
      "colours")
    sender.messages.map { _.subject } shouldBe Seq(
      "ideas for my design",
      "ideas for my design",
      "ideas for my design",
      "ideas for my design")
  }

  it("sends case classes to a default destination/subject") {
    val sender = new MemoryMessageSender() {
      override val destination = "trees"
      override val subject = "ideas for my garden"
    }

    case class Tree(name: String)

    sender.sendT(Tree("oak")) shouldBe Success(())
    sender.sendT(Tree("ash")) shouldBe Success(())
    sender.sendT(Tree("yew")) shouldBe Success(())

    sender.messages.map { _.destination } shouldBe Seq(
      "trees",
      "trees",
      "trees")
    sender.messages.map { _.subject } shouldBe Seq(
      "ideas for my garden",
      "ideas for my garden",
      "ideas for my garden")
  }

  it(
    "sends type-parameter encoded case classes to a default destination/subject") {
    val sender = new MemoryMessageSender()

    containers.map { c =>
      sender.sendT[Container](c) shouldBe Success(())
    }

    containers.zip(sender.messages).map {
      case (container, message) =>
        fromJson[Container](message.body).get shouldBe container
    }
  }

}
