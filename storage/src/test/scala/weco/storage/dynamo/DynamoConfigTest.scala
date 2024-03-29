package weco.storage.dynamo

import javax.naming.ConfigurationException
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DynamoConfigTest extends AnyFunSpec with Matchers {
  it("allows looking up the index") {
    val index = "myindex"
    val config = DynamoConfig(tableName = "mytable", indexName = index)
    config.indexName shouldBe index
  }

  it(
    "throws a ConfigurationException if you look up the index without setting it") {
    val config = DynamoConfig(tableName = "mytable")
    config.maybeIndexName shouldBe None

    val caught = intercept[ConfigurationException] {
      config.indexName
    }
    caught.getMessage shouldBe "Tried to look up the index, but no index is configured!"
  }
}
