package uk.ac.wellcome.storage.fixtures

import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scanamo.query.UniqueKey
import org.scanamo.syntax._
import org.scanamo.{DynamoFormat, DynamoReadError, Scanamo, Table => ScanamoTable}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{
  AttributeDefinition,
  CreateTableRequest,
  DeleteTableRequest,
  DescribeTableRequest,
  KeySchemaElement,
  KeyType,
  ProvisionedThroughput,
  ScalarAttributeType
}
import uk.ac.wellcome.fixtures._
import uk.ac.wellcome.storage.dynamo.{DynamoClientFactory, DynamoConfig}

import scala.collection.immutable

object DynamoFixtures {
  case class Table(name: String, index: String)
}

trait DynamoFixtures
    extends Eventually
    with Matchers
    with IntegrationPatience
    with RandomGenerators {
  import DynamoFixtures._

  private val port = 45678
  private val dynamoDBEndPoint = "http://localhost:" + port
  private val regionName = "localhost"
  private val accessKey = "access"
  private val secretKey = "secret"

  implicit val dynamoClient: DynamoDbClient = DynamoClientFactory.create(
    region = regionName,
    endpoint = dynamoDBEndPoint,
    accessKey = accessKey,
    secretKey = secretKey
  )

  implicit val scanamo = Scanamo(dynamoClient)

  def nonExistentTable: Table =
    Table(
      name = randomAlphanumeric(),
      index = randomAlphanumeric()
    )

  def withSpecifiedLocalDynamoDbTable[R](
    createTable: DynamoDbClient => Table): Fixture[Table, R] =
    fixture[Table, R](
      create = createTable(dynamoClient),
      destroy = { table =>
        dynamoClient.deleteTable(
          DeleteTableRequest.builder().tableName(table.name).build()
        )
      }
    )

  def withSpecifiedTable[R](
    tableDefinition: Table => Table): Fixture[Table, R] = fixture[Table, R](
    create = {
      val tableName = randomAlphanumeric()
      val indexName = randomAlphanumeric()

      tableDefinition(Table(tableName, indexName))
    },
    destroy = { table =>
      dynamoClient.deleteTable(
        DeleteTableRequest.builder().tableName(table.name).build()
      )
    }
  )

  def withLocalDynamoDbTable[R](testWith: TestWith[Table, R]): R =
    withSpecifiedTable(createTable) { table =>
      testWith(table)
    }

  def createTable(table: DynamoFixtures.Table): Table

  def putTableItem[T: DynamoFormat](item: T, table: Table): Unit =
    scanamo.exec(
      ScanamoTable[T](table.name).put(item)
    )

  def putTableItems[T: DynamoFormat](items: Seq[T], table: Table): Unit =
    scanamo.exec(
      ScanamoTable[T](table.name).putAll(items.toSet)
    )

  def getTableItem[T: DynamoFormat](
    id: String,
    table: Table): Option[Either[DynamoReadError, T]] =
    scanamo.exec(
      ScanamoTable[T](table.name).get("id" === id)
    )

  def deleteTableItem[T: DynamoFormat](key: UniqueKey[_], table: Table): Unit =
    scanamo.exec(
      ScanamoTable[T](table.name).delete(key)
    )

  def getExistingTableItem[T: DynamoFormat](id: String, table: Table): T = {
    val record = getTableItem[T](id, table)
    record shouldBe 'defined
    record.get shouldBe 'right
    record.get.right.get
  }

  def scanTable[T: DynamoFormat](
    table: Table): immutable.Seq[Either[DynamoReadError, T]] =
    scanamo.exec(
      ScanamoTable[T](table.name).scan()
    )

  def createDynamoConfigWith(table: Table): DynamoConfig =
    DynamoConfig(
      tableName = table.name,
      maybeIndexName = Some(table.index)
    )

  def createTableFromRequest(table: Table,
                             requestBuilder: CreateTableRequest.Builder): Table = {
    val request =
      requestBuilder
        .provisionedThroughput(
          ProvisionedThroughput.builder()
            .readCapacityUnits(1L)
            .writeCapacityUnits(1L)
            .build()
        )
        .build()

    dynamoClient.createTable(request)

    // See https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/waiters.html
    val waiter = dynamoClient.waiter()
    waiter.waitUntilTableExists(
      DescribeTableRequest.builder().tableName(table.name).build()
    )

    table
  }

  def createTableWithHashKey(
    table: Table,
    keyName: String = "id",
    keyType: ScalarAttributeType = ScalarAttributeType.S
  ): Table =
    createTableFromRequest(
      table = table,
      CreateTableRequest.builder()
        .tableName(table.name)
        .keySchema(
          KeySchemaElement.builder()
            .attributeName(keyName)
            .keyType(KeyType.HASH)
            .build())
        .attributeDefinitions(
          AttributeDefinition.builder()
            .attributeName(keyName)
            .attributeType(keyType)
            .build()
        )
    )

  def createTableWithHashRangeKey(
    table: Table,
    hashKeyName: String = "id",
    hashKeyType: ScalarAttributeType = ScalarAttributeType.S,
    rangeKeyName: String = "version",
    rangeKeyType: ScalarAttributeType = ScalarAttributeType.N): Table =
    createTableFromRequest(
      table = table,
      CreateTableRequest.builder()
        .tableName(table.name)
        .keySchema(
          KeySchemaElement.builder()
            .attributeName(hashKeyName)
            .keyType(KeyType.HASH)
            .build(),
          KeySchemaElement.builder()
            .attributeName(rangeKeyName)
            .keyType(KeyType.RANGE)
            .build()
        )
        .attributeDefinitions(
          AttributeDefinition.builder()
            .attributeName(hashKeyName)
            .attributeType(hashKeyType)
            .build(),
          AttributeDefinition.builder()
            .attributeName(rangeKeyName)
            .attributeType(rangeKeyType)
            .build()
        )
    )
}
