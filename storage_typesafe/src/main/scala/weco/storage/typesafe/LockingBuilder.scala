package weco.storage.typesafe

import com.typesafe.config.Config
import weco.storage.locking.dynamo.{DynamoLockDao, DynamoLockingService}
import weco.storage.locking.dynamo.DynamoLockDao

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object LockingBuilder {
  def buildDynamoLockingService[Out, OutMonad[_]](config: Config,
                                                  namespace: String = "")(
    implicit ec: ExecutionContext): DynamoLockingService[Out, OutMonad] = {
    implicit val dynamoLockDao: DynamoLockDao =
      DynamoLockDaoBuilder.buildDynamoLockDao(config, namespace = namespace)

    new DynamoLockingService()
  }
}
