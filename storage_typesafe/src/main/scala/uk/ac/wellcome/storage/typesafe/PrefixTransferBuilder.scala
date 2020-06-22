package uk.ac.wellcome.storage.typesafe

import com.typesafe.config.Config
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

object PrefixTransferBuilder {
  import CloudProvider._

  //  ## Expects HOCON config like:
  //  {
  //    cloudProvider = "aws"
  //    aws.s3.region = "eu-west-1"
  //  }
  //  ## OR
  //  {
  //    cloudProvider = "azure"
  //    azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
  //  }
  //

  def build(config: Config) = {
    val cloudProvider = config
      .getStringOption("cloudProvider")
      .flatMap(CloudProvider.create)
      .getOrElse(AWS)

    
  }

}
