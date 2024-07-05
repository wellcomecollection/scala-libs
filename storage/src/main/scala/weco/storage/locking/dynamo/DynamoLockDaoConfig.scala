package weco.storage.locking.dynamo

import weco.storage.dynamo.DynamoConfig

import scala.concurrent.duration.Duration

case class DynamoLockDaoConfig(
  dynamoConfig: DynamoConfig,
  expiryTime: Duration
)
