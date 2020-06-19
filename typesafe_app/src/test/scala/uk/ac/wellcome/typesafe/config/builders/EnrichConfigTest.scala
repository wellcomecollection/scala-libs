package uk.ac.wellcome.typesafe.config.builders

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.OptionValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EnrichConfigTest extends AnyFunSpec with Matchers with OptionValues {

  import EnrichConfig._

  val configString =
    """
      |example {
      |  config {
      |    string = "somevalue"
      |    int = 1
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

  describe("getStringOrElse") {
    it("returns the value when a path is available") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getStringOrElse("example.config.string")("default") shouldBe "somevalue"
    }

    it("returns the default value when a path is unavailable") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getStringOrElse("example.config.nopath")("default") shouldBe "default"
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

  describe("getIntOrElse") {
    it("returns the value when a path is available") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getIntOrElse("example.config.int")(10) shouldBe 1
    }

    it("returns the default value when a path is unavailable") {
      val myConfig = ConfigFactory.parseString(configString)

      myConfig.getIntOrElse("example.config.nopath")(10) shouldBe 10
    }
  }

  it("elides .. to . when reading config paths") {
    val myConfig: Config = ConfigFactory.parseString(configString)

    myConfig.getStringOption("example.tooManyDots..config") shouldBe Some("tooManyDots")
  }
}
