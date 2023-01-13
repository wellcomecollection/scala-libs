package weco.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{RequestFailure, Response}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.json.JsonUtil._
import weco.json.utils.JsonAssertions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TestObject(
  id: String,
  description: String,
  visible: Boolean
)

case class CompatibleTestObject(
  id: String,
  description: String,
  visible: Boolean,
  count: Int
)

case class BadTestObject(
  id: String,
  weight: Int
)

class ElasticsearchIndexCreatorTest
    extends AnyFunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonAssertions
    with BeforeAndAfterEach {

  val indexFields = Seq(
    keywordField("id"),
    textField("description"),
    booleanField("visible")
  )

  val testIndexConfig = IndexConfig(
    mapping = properties(
      Seq(
        keywordField("id"),
        textField("description"),
        booleanField("visible")
      )).dynamic(DynamicMapping.Strict),
    analysis = Analysis(Nil)
  )

  val compatibleTestIndexConfig = IndexConfig(
    mapping = properties(
      Seq(
        keywordField("id"),
        textField("description"),
        booleanField("visible"),
        intField("count")
      )).dynamic(DynamicMapping.Strict),
    analysis = Analysis(Nil)
  )

  it("creates an index into which doc of the expected type can be put") {
    withLocalElasticsearchIndex(testIndexConfig) { index =>
      val testObject = TestObject("id", "description", visible = true)
      val testObjectJson = toJson(testObject).get

      eventually {
        for {
          _ <- elasticClient.execute(indexInto(index.name).doc(testObjectJson))
          response: Response[SearchResponse] <- elasticClient
            .execute {
              search(index).matchAllQuery()
            }
        } yield {
          val hits = response.result.hits.hits
          hits should have size 1

          assertJsonStringsAreEqual(
            hits.head.sourceAsString,
            testObjectJson
          )
        }
      }
    }
  }

  it("creates metadata when creating an index") {
    val customMeta = Map("bleurgh" -> "blargh")
    val indexConfigWithMetadata = IndexConfig(
      mapping = properties().meta(customMeta),
      analysis = Analysis(Nil))

    withLocalElasticsearchIndex(indexConfigWithMetadata) { index =>
      whenReady(elasticClient.execute(getMapping(index.name))) { mapping =>
        mapping.result.head.meta shouldBe customMeta
      }
    }
  }

  it("merges metadata when updating an index") {
    val customMeta1 = Map("versions.1" -> 1)
    val customMeta2 = Map("versions.2" -> 2)

    val indexConfigWithMetadata1 = IndexConfig(
      mapping = properties().meta(customMeta1),
      analysis = Analysis(Nil))
    val indexConfigWithMetadata2 = IndexConfig(
      mapping = properties().meta(customMeta2),
      analysis = Analysis(Nil))

    withLocalElasticsearchIndex(indexConfigWithMetadata1) { index =>
      withLocalElasticsearchIndex(indexConfigWithMetadata2, index = index) {
        _ =>
          whenReady(elasticClient.execute(getMapping(index.name))) { mapping =>
            mapping.result.head.meta shouldBe Map(
              "versions.1" -> 1,
              "versions.2" -> 2)
          }
      }
    }
  }

  it("create an index where inserting a doc of an unexpected type fails") {
    withLocalElasticsearchIndex(testIndexConfig) { index =>
      val badTestObject = BadTestObject("id", 5)
      val badTestObjectJson = toJson(badTestObject).get

      val future: Future[Response[IndexResponse]] =
        elasticClient
          .execute {
            indexInto(index.name)
              .doc(badTestObjectJson)
          }

      whenReady(future) { response =>
        response.isError shouldBe true
        response shouldBe a[RequestFailure]
      }
    }
  }

  it("updates an already existing index with a compatible mapping") {
    withLocalElasticsearchIndex(testIndexConfig) { index =>
      withLocalElasticsearchIndex(compatibleTestIndexConfig, index = index) {
        _ =>
          val compatibleTestObject = CompatibleTestObject(
            id = "id",
            description = "description",
            count = 5,
            visible = true
          )

          val compatibleTestObjectJson = toJson(compatibleTestObject).get

          val futureInsert: Future[Response[IndexResponse]] =
            elasticClient
              .execute {
                indexInto(index.name)
                  .doc(compatibleTestObjectJson)
              }

          whenReady(futureInsert) { response =>
            if (response.isError) { println(response) }
            response.isError shouldBe false

            eventually {
              val response: Response[SearchResponse] =
                elasticClient.execute {
                  search(index).matchAllQuery()
                }.await

              val hits = response.result.hits.hits

              hits should have size 1

              assertJsonStringsAreEqual(
                hits.head.sourceAsString,
                compatibleTestObjectJson
              )
            }
          }
      }
    }
  }
}
