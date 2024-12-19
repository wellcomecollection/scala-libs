package weco.elasticsearch.typesafe

import com.typesafe.config.ConfigFactory
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.RandomGenerators

class ElasticBuilderTest
    extends AnyFunSpec
    with Matchers
    with RandomGenerators {

  val List(namespace, host, username, password, apiKey) =
    1.to(5).map(_ => randomAlphanumeric()).toList
  val defaultPort = 9200
  val defaultProtocol = "http"

  describe("when username and password are specified") {
    val usernamePasswordConfig = ConfigFactory.parseString(
      f"""
         |es.$namespace.host = $host
         |es.$namespace.username = $username
         |es.$namespace.password = $password
         |""".stripMargin
    )

    it("can build and accept ElasticConfigUsernamePassword") {
      val elasticConfig = ElasticBuilder.buildElasticClientConfig(
        usernamePasswordConfig,
        namespace)

      elasticConfig shouldBe ElasticConfigUsernamePassword(
        host = host,
        port = defaultPort,
        protocol = defaultProtocol,
        username = username,
        password = password
      )

      noException shouldBe thrownBy(
        ElasticBuilder.buildElasticClient(elasticConfig)
      )
    }

    it("can build a client directly") {
      noException shouldBe thrownBy(
        ElasticBuilder.buildElasticClient(usernamePasswordConfig, namespace)
      )
    }
  }

  val apiKeyConfig = ConfigFactory.parseString(
    f"""
      |es.$namespace.host = $host
      |es.$namespace.apikey = $apiKey
      |""".stripMargin
  )

  describe("when an API key is specified") {
    it("can build and accept ElasticConfigApiKey") {
      val elasticConfig =
        ElasticBuilder.buildElasticClientConfig(apiKeyConfig, namespace)

      elasticConfig shouldBe ElasticConfigApiKey(
        host = host,
        port = defaultPort,
        protocol = defaultProtocol,
        apiKey = apiKey
      )

      noException shouldBe thrownBy(
        ElasticBuilder.buildElasticClient(elasticConfig)
      )
    }

    it("builds a client directly") {
      noException shouldBe thrownBy(
        ElasticBuilder.buildElasticClient(apiKeyConfig, "test")
      )
    }
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
