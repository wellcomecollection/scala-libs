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
      |  config1 {
      |    somekey1 = "somevalue"
      |    somekey2 = 1
      |  }
      |  config2.tooManyDots = "tooManyDots"
      |}
      |""".stripMargin

  describe("getOrElse") {
    it("returns the value when a path is available") {
      val myConfig: Config = ConfigFactory.parseString(configString)

      myConfig.getOrElse("example.config1.somekey1")("default") shouldBe "somevalue"
    }

    it("returns the default value when a path is unavailable") {
      val myConfig: Config = ConfigFactory.parseString(configString)

      myConfig.getOrElse("example.config1.nokey")("default") shouldBe "default"
    }
  }

  describe("get") {
    it("returns None when a path is unavailable") {
      val myConfig: Config = ConfigFactory.parseString(configString)

      myConfig.get[String]("example.nopath") shouldBe None
    }

    it("fails because this doesnt work") {
      val myConfig: Config = ConfigFactory.parseString(configString)

      val stringResult = myConfig.get[String]("example.config1.somekey2")
      val intResult = myConfig.get[Int]("example.config1.somekey2")

      stringResult shouldBe Some("1")
      intResult shouldBe Some(1)
    }

    it("returns Some(value) when a path is available") {
      val myConfig: Config = ConfigFactory.parseString(configString)

      myConfig.get[String]("example.config1.somekey1") shouldBe Some("somevalue")
    }

    it("elides .. to . when reading config paths") {
      val myConfig: Config = ConfigFactory.parseString(configString)

      myConfig.get[String]("example.config2..tooManyDots") shouldBe Some("tooManyDots")
    }
  }

}
