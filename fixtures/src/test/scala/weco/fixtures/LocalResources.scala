package weco.fixtures

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

trait LocalResources {
  /** Returns the contents of a file in the "resources" folder. */
  def readResource(name: String): String =
    Source.fromResource(name).getLines.mkString("\n")
}

class LocalResourcesTest extends AnyFunSpec with Matchers with LocalResources {
  it("reads a file") {
    readResource("greetings.txt") shouldBe "hello world\nbonjour monde\nHallo Welt"
  }
}