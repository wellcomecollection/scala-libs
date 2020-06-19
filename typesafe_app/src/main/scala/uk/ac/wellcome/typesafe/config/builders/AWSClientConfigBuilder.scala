package uk.ac.wellcome.typesafe.config.builders

import com.typesafe.config.Config
import uk.ac.wellcome.config.models.AWSClientConfig

import EnrichConfig._

trait AWSClientConfigBuilder {
  protected def buildAWSClientConfig(config: Config,
                                     namespace: String): AWSClientConfig = {
    val accessKey = config.getStringOption(s"aws.$namespace.key")
    val secretKey = config.getStringOption(s"aws.$namespace.secret")
    val endpoint = config.getStringOption(s"aws.$namespace.endpoint")
    val maxConnections = config.getStringOption(s"aws.$namespace.max-connections")
    val region = config.getStringOption(s"aws.$namespace.region")
      .getOrElse("eu-west-1")

    AWSClientConfig(
      accessKey = accessKey,
      secretKey = secretKey,
      endpoint = endpoint,
      maxConnections = maxConnections,
      region = region
    )
  }
}
