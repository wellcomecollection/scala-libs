package weco.typesafe.config.builders

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.OptionValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._

class EnrichConfigTest extends AnyFunSpec with Matchers with OptionValues {

  import EnrichConfig._

  val configString =
    """
      |example {
      |  config {
      |    string = "somevalue"
      |    int = 1
      |    duration = "2 second"
      |  }
      |  tooManyDots {
      |   config = "tooManyDots"
      |  }
      |}
      |""".stripMargin

  describe("getStringOption") {
    it("returns the value when a path is available") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getStringOption("example.config.string") shouldBe Some("somevalue")
    }

    it("returns None when a path is unavailable") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getStringOption("example.config.nopath") shouldBe None
    }
  }

  describe("getIntOption") {
    it("returns the value when a path is available") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getIntOption("example.config.int")shouldBe Some(1)
    }

    it("returns None when a path is unavailable") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getIntOption("example.config.nopath") shouldBe None
    }
  }

  describe("getBooleanOption") {
    val config = ConfigFactory.parseString(
      """
        |islander1 {
        |  alwaysTellsTruth = true
        |}
        |islander2 {
        |  alwaysTellsTruth = false
        |}
        |islander3 {}
        |""".stripMargin
    )

    it("recognises true/false values") {
      config.getBooleanOption("islander1.alwaysTellsTruth") shouldBe Some(true)
      config.getBooleanOption("islander2.alwaysTellsTruth") shouldBe Some(false)
    }

    it("returns None for a missing value") {
      config.getBooleanOption("islander3.alwaysTellsTruth") shouldBe None
    }
  }

  describe("getDurationOption") {
    it("returns the value when a path is available") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getDurationOption("example.config.duration") shouldBe Some(2.seconds)
    }

    it("returns None when a path is unavailable") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getDurationOption("example.config.nopath") shouldBe None
    }
  }

  it("elides .. to . when reading config paths") {
    val myConfig: Config = ConfigFactory.parseString(configString)

    myConfig.getStringOption("example.tooManyDots..config") shouldBe Some("tooManyDots")
  }
}
