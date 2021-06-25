package weco.typesafe

import com.typesafe.config.Config
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class WellcomeTypesafeAppTest extends AnyFunSpec with Matchers {
  var calledWith: Option[Config] = None

  class DummyRunnable(config: Config) extends Runnable {
    override def run(): Future[Any] = Future {
      calledWith = Some(config)

      info("that I shall say goodnight till it be morrow")
    }
  }

  object DummyWellcomeTypesafeApp extends WellcomeTypesafeApp {
    runWithConfig { config: Config =>
      info("parting is such sweet sorrow")

      new DummyRunnable(config)
    }

    override protected def exit(): Unit = ()
  }

  describe("when main is called") {
    DummyWellcomeTypesafeApp.main(Array.empty)

    val config = calledWith.get

    it("calls run() in the set Runnable") {
      calledWith shouldBe a[Some[_]]
    }

    it("sets the expected logging config") {
      val loggers = config.getList("akka.loggers")
      loggers.size() shouldBe 1
      loggers.get(0).unwrapped() shouldBe "akka.event.slf4j.Slf4jLogger"

      val loggingFilter = config.getString("akka.logging-filter")
      loggingFilter shouldBe "akka.event.slf4j.Slf4jLoggingFilter"
    }

    it("sets the expected overridden config") {
      val overriddenConfig = config.getString("overridden.config")
      overriddenConfig shouldBe "value"
    }
  }
}
