package weco.elasticsearch.typesafe

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ElasticBuilderTest extends AnyFunSpec with Matchers {
  it("builds a client when a username and password are specified") {
    val config = ConfigFactory.parseString(
      """
        |es.test.host = test.elastic
        |es.test.username = test-username
        |es.test.password = test-password
        |""".stripMargin
    )

    noException shouldBe thrownBy(
      ElasticBuilder.buildElasticClient(config, "test")
    )
  }

  it("builds a client when an API key is specified") {
    val config = ConfigFactory.parseString(
      """
        |es.test.host = test.elastic
        |es.test.apikey = test-key
        |""".stripMargin
    )

    noException shouldBe thrownBy(
      ElasticBuilder.buildElasticClient(config, "test")
    )
  }

  it("errors if there is not enough config to build a client") {
    val config = ConfigFactory.parseString(
      """
        |es.test.host = test.elastic
        |es.test.username = test-username
        |""".stripMargin
    )

    a[Throwable] shouldBe thrownBy(
      ElasticBuilder.buildElasticClient(config, "test")
    )
  }
}
