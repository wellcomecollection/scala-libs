package uk.ac.wellcome.typesafe

import com.typesafe.config.{Config, ConfigFactory}

trait WellcomeTypesafeApp extends WellcomeApp {
  def runWithConfig(builder: Config => Runnable) = {
    val config: Config = ConfigFactory.load()
    val workerService = builder(config)
    run(workerService)
  }
}
