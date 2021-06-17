package uk.ac.wellcome.elasticsearch

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
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions

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

  object TestIndexConfig extends IndexConfig {
    val mapping = properties(
      Seq(
        keywordField("id"),
        textField("description"),
        booleanField("visible")
      )).dynamic(DynamicMapping.Strict)
    val analysis = Analysis(Nil)
  }

  object CompatibleTestIndexConfig extends IndexConfig {
    val mapping = properties(
      Seq(
        keywordField("id"),
        textField("description"),
        booleanField("visible"),
        intField("count")
      )).dynamic(DynamicMapping.Strict)
    val analysis = Analysis(Nil)
  }

  it("creates an index into which doc of the expected type can be put") {
    withLocalElasticsearchIndex(TestIndexConfig) { index =>
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

  it("creates metadata when creating an index"){
    val customMeta = Map("bleurgh" -> "blargh")

    object IndexConfigWithMetadata extends IndexConfig {
      val mapping = properties().meta(customMeta)
      val analysis = Analysis(Nil)
    }
    withLocalElasticsearchIndex(IndexConfigWithMetadata) { index =>
    whenReady(elasticClient.execute(getMapping(index.name))){mapping =>
      mapping.result.head.meta shouldBe customMeta
    }
  }}

  it("merges metadata when updating an index"){
    val customMeta1 = Map("versions.1" -> 1)
    val customMeta2 = Map("versions.2" -> 2)

    object IndexConfigWithMetadata1 extends IndexConfig {
      val mapping = properties().meta(customMeta1)
      val analysis = Analysis(Nil)
    }
    object IndexConfigWithMetadata2 extends IndexConfig {
      val mapping = properties().meta(customMeta2)
      val analysis = Analysis(Nil)
    }
    withLocalElasticsearchIndex(IndexConfigWithMetadata1) { index =>
      withLocalElasticsearchIndex(IndexConfigWithMetadata2, index = index) { _ =>
        whenReady(elasticClient.execute(getMapping(index.name))){mapping =>
          mapping.result.head.meta shouldBe Map("versions.1" -> 1, "versions.2" -> 2)
        }
      }
    }}


  it("create an index where inserting a doc of an unexpected type fails") {
    withLocalElasticsearchIndex(TestIndexConfig) { index =>
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
    withLocalElasticsearchIndex(TestIndexConfig) { index =>
      withLocalElasticsearchIndex(CompatibleTestIndexConfig, index = index) {
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

  object RefreshIntervalIndexConfig extends IndexConfig {
    val analysis: Analysis = Analysis(analyzers = List())
    val mapping = properties()
    override val refreshInterval = RefreshInterval.Off
  }

  it("sets initial refresh_interval on a non-existing index") {
    withLocalElasticsearchIndex(RefreshIntervalIndexConfig) { index =>
      val resp = elasticClient.execute {
        getSettings(index.name)
      }.await

      resp.result.settings(index.name).get("index.refresh_interval") shouldBe Some("-1")
    }
  }

  it("updates the refresh_interval on an already existing index") {
    withLocalElasticsearchIndex(RefreshIntervalIndexConfig) { index =>
      val resp = elasticClient.execute {
        getSettings(index.name)
      }.await

      resp.result.settings(index.name).get("index.refresh_interval") shouldBe Some("-1")
    }
  }
}
