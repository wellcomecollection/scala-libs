package uk.ac.wellcome.storage.locking.dynamo

import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.concurrent.duration.Duration

case class DynamoLockDaoConfig(
  dynamoConfig: DynamoConfig,
  expiryTime: Duration
)
