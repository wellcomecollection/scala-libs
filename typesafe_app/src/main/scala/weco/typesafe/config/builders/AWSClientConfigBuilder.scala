package weco.typesafe.config.builders

import com.typesafe.config.Config
import weco.config.models.AWSClientConfig
import EnrichConfig._

trait AWSClientConfigBuilder {
  protected def buildAWSClientConfig(config: Config,
                                     namespace: String): AWSClientConfig = {
    val accessKey = config.getString("key", namespace = namespace)
    val secretKey = config.getString("secret", namespace = namespace)
    val endpoint = config.getString("endpoint", namespace = namespace)
    val maxConnections = config.getString("max-connections", namespace = namespace)
    val region = config
      .getString("region", namespace = namespace)
      .getOrElse("eu-west-1")

    AWSClientConfig(
      accessKey = accessKey,
      secretKey = secretKey,
      endpoint = endpoint,
      maxConnections = maxConnections,
      region = region
    )
  }

  implicit class AwsConfigOps(config: Config) {
    def getString(key: String, namespace: String): Option[String] =
      config.getStringOption(s"aws.$namespace.$key") match {
        case Some(value) => Some(value)
        case None        => config.getStringOption(s"aws.$key")
      }
  }
}
