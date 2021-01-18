package uk.ac.wellcome.storage.locking.dynamo

import java.time.Duration
import java.util.UUID
import org.scalatest.Assertion
import org.scanamo.generic.auto._
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{
  AttributeDefinition,
  CreateTableRequest,
  GlobalSecondaryIndex,
  KeySchemaElement,
  KeyType,
  Projection,
  ProjectionType,
  ProvisionedThroughput
}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.fixtures.DynamoFixtures
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.locking.{LockDao, LockDaoFixtures}
import uk.ac.wellcome.storage.dynamo.DynamoTimeFormat._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

trait DynamoLockDaoFixtures
    extends LockDaoFixtures[String, UUID, Table]
    with DynamoFixtures {
  def createTable(table: Table): Table =
    createLockTable(table)

  override def withLockDaoContext[R](testWith: TestWith[Table, R]): R =
    withLocalDynamoDbTable { table =>
      testWith(table)
    }

  override def withLockDao[R](lockTable: Table)(
    testWith: TestWith[LockDao[String, UUID], R]): R =
    withLockDao(dynamoClient, lockTable = lockTable) { lockDao =>
      testWith(lockDao)
    }

  override def createIdent: String = randomAlphanumeric()
  override def createContextId: UUID = randomUUID

  def assertNoLocks(lockTable: Table): Assertion =
    scanTable[ExpiringLock](lockTable) shouldBe empty

  def withLockDao[R](
    dynamoClient: DynamoDbClient,
    lockTable: Table,
    seconds: Int = 180)(testWith: TestWith[DynamoLockDao, R]): R = {
    val rowLockDaoConfig = DynamoLockDaoConfig(
      dynamoConfig = createDynamoConfigWith(lockTable),
      expiryTime = Duration.ofSeconds(seconds)
    )

    val dynamoLockDao = new DynamoLockDao(
      client = dynamoClient,
      config = rowLockDaoConfig
    )

    testWith(dynamoLockDao)
  }

  def withLockDao[R](dynamoClient: DynamoDbClient)(
    testWith: TestWith[DynamoLockDao, R]): R =
    withLocalDynamoDbTable { lockTable =>
      withLockDao(dynamoClient, lockTable) { lockDao =>
        testWith(lockDao)
      }
    }

  def withLockDao[R](lockTable: Table, seconds: Int)(
    testWith: TestWith[DynamoLockDao, R]): R =
    withLockDao(dynamoClient, lockTable = lockTable, seconds = seconds) {
      lockDao =>
        testWith(lockDao)
    }

  def createLockTable(table: Table): Table =
    createTableFromRequest(
      table,
      CreateTableRequest.builder()
        .tableName(table.name)
        .keySchema(
          KeySchemaElement.builder()
            .attributeName("id")
            .keyType(KeyType.HASH)
            .build()
        )
        .attributeDefinitions(
          AttributeDefinition.builder()
            .attributeName("id")
            .attributeType("S")
            .build(),
          AttributeDefinition.builder()
            .attributeName("contextId")
            .attributeType("S")
            .build()
        )
        .globalSecondaryIndexes(
          GlobalSecondaryIndex.builder()
            .indexName(table.index)
            .projection(
              Projection.builder().projectionType(ProjectionType.ALL).build()
            )
            .keySchema(
              KeySchemaElement.builder()
                .attributeName("contextId")
                .keyType(KeyType.HASH)
                .build()
            )
            .provisionedThroughput(
              ProvisionedThroughput.builder()
                .readCapacityUnits(1L)
                .writeCapacityUnits(1L)
                .build()
            )
            .build()
        )
    )
}
