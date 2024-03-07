package weco

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class LoggingTest extends AnyFunSpec with Matchers {
    case class LogThing() extends Logging {

      val foo = Map(
        "foo" -> "bar",
        "baz" -> "quux",
        "quux" -> "baz"
      )
      val bar = new Throwable("foo")
        def logError(): Unit = {
            error("This is an error message", foo, bar)
        }
    }

    describe("Logging") {
        it("should fail") {
            val logThing = LogThing()

            logThing.logError()

            true should be(false)
        }
    }
}
