package weco.typesafe

import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait WellcomeTypesafeApp extends App with Logging {
  protected def run(service: Runnable) =
    Await.result(service.run(), Duration.Inf)

  protected def exit(statusCode: Int = 0): Unit =
    System.exit(statusCode)

  def runWithConfig(builder: Config => Runnable) =
    try {
      info("Starting service")

      run {
        builder(ConfigFactory.load)
      }

      info("Shutting down service")
      exit(0)
    } catch {
      case e: Throwable =>
        error("Fatal error, terminating service:", e)
        exit(1)
    }
}
