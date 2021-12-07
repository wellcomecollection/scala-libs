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

  trait DummyWellcomeTypesafeApp extends WellcomeTypesafeApp {
    var exitStatus: Option[Int] = None

    override protected def exit(statusCode: Int = 0): Unit = {
      info(statusCode)
      exitStatus = Some(statusCode)
    }
  }

  object NormalDummyWellcomeTypesafeApp extends DummyWellcomeTypesafeApp {
    runWithConfig { config: Config =>
      info("parting is such sweet sorrow")

      new DummyRunnable(config)
    }
  }

  object ExplodingDummyWellcomeTypesafeApp extends DummyWellcomeTypesafeApp {
    runWithConfig { config: Config =>
      throw new Exception("O happy dagger!")

      new DummyRunnable(config)
    }
  }

  describe("when a service starts") {
    NormalDummyWellcomeTypesafeApp.main(Array.empty)

    it("calls run() in the set Runnable") {
      calledWith shouldBe a[Some[_]]
    }

    it("sets the expected logging config") {
      val loggers = calledWith.get.getList("akka.loggers")
      loggers.size() shouldBe 1
      loggers.get(0).unwrapped() shouldBe "akka.event.slf4j.Slf4jLogger"

      val loggingFilter = calledWith.get.getString("akka.logging-filter")
      loggingFilter shouldBe "akka.event.slf4j.Slf4jLoggingFilter"
    }

    it("sets the expected overridden config") {
      val overriddenConfig = calledWith.get.getString("overridden.config")
      overriddenConfig shouldBe "value"
    }

    it("calls exit with status 0") {
      NormalDummyWellcomeTypesafeApp.exitStatus shouldBe Some(0)
    }
  }

  describe("when a service fails to start") {
    ExplodingDummyWellcomeTypesafeApp.main(Array.empty)

    it("calls exit with status 1") {
      ExplodingDummyWellcomeTypesafeApp.exitStatus shouldBe Some(1)
    }
  }
}
