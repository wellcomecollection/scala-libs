package weco.fixtures

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.{Failure, Success, Try}

trait LocalResources {

  /** Returns the contents of a file in the "resources" folder. */
  def readResource(name: String): String = {
    val resource = Source.fromResource(name)

    Try {
      resource.getLines()
    } match {
      case Success(lines) => lines.mkString("\n")

      case Failure(_: NullPointerException) if name.startsWith("/") =>
        throw new RuntimeException(s"Could not find resource `$name`; try removing the leading slash")
      case Failure(_: NullPointerException) =>
        throw new RuntimeException(s"Could not find resource `$name`")

      case Failure(e) => throw e
    }
  }
}

class LocalResourcesTest extends AnyFunSpec with Matchers with LocalResources {
  it("reads a file") {
    readResource("greetings.txt") shouldBe "hello world\nbonjour monde\nHallo Welt"
  }

  it("prompts you to remove a leading slash") {
    val result = Try { readResource("/greetings.txt") }

    result.failed.get.getMessage shouldBe "Could not find resource `/greetings.txt`; try removing the leading slash"
  }

  it("throws a meaningful error if the file doesn't exist") {
    val result = Try { readResource("doesnotexist.txt") }

    result.failed.get.getMessage shouldBe "Could not find resource `doesnotexist.txt`"
  }
}
