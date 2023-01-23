package weco.storage.typesafe

import com.typesafe.config.Config
import weco.storage.dynamo.DynamoConfig
import weco.typesafe.config.builders.EnrichConfig._

object DynamoBuilder {
  def buildDynamoConfig(config: Config,
                        namespace: String = ""): DynamoConfig = {
    val tableName = config
      .requireString(s"aws.$namespace.dynamo.tableName")
    val tableIndex = config
      .getStringOption(s"aws.$namespace.dynamo.tableIndex")
      .getOrElse("")

    DynamoConfig(
      tableName = tableName,
      maybeIndexName = if (tableIndex.isEmpty) None else Some(tableIndex)
    )
  }
}
