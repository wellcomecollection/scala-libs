package weco.typesafe.config.builders

import com.typesafe.config.Config
import weco.config.models.AWSClientConfig
import EnrichConfig._

trait AWSClientConfigBuilder {
  protected def buildAWSClientConfig(config: Config,
                                     namespace: String): AWSClientConfig = {
    val accessKey = config.getAwsString("key", namespace = namespace)
    val secretKey = config.getAwsString("secret", namespace = namespace)
    val endpoint = config.getAwsString("endpoint", namespace = namespace)
    val maxConnections =
      config.getAwsString("max-connections", namespace = namespace)
    val region = config
      .getAwsString("region", namespace = namespace)
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
    def getAwsString(key: String, namespace: String): Option[String] =
      config.getStringOption(s"aws.$namespace.$key") match {
        case Some(value) => Some(value)
        case None        => config.getStringOption(s"aws.$key")
      }
  }
}
