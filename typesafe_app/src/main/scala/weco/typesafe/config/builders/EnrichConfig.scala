package weco.typesafe.config.builders

import com.typesafe.config.Config

import scala.concurrent.duration.Duration

object EnrichConfig {

  implicit class RichConfig(val underlying: Config) extends AnyVal {
    private def cleanUpPath(p: String): String =
      // Sometimes we may get a path that features two double dots, if there's an
      // empty namespace -- in this case, elide the two dots into one.
      p.replaceAllLiterally("..", ".")

    private def getPathValue[T](path: String)(f: String => T): Option[T] = {
      val cleanPath = cleanUpPath(path)

      if (underlying.hasPath(cleanPath)) {
        Some(f(cleanPath))
      } else {
        None
      }
    }

    private def emergencyStop(path: String): Unit = {
      println(s"No value found for path ${cleanUpPath(path)}")
      System.exit(1)
    }

    def getStringOption(path: String): Option[String] =
      getPathValue(path) {
        underlying.getString
      }

    def requireString(path: String): String =
      getStringOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }

    def getIntOption(path: String): Option[Int] =
      getPathValue(path) {
        underlying.getInt
      }

    def requireInt(path: String): Int =
      getIntOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }

    def getBooleanOption(path: String): Option[Boolean] =
      getPathValue(path) {
        underlying.getBoolean
      }

    def getDurationOption(path: String): Option[Duration] =
      getPathValue(path) { s =>
        // The getDuration method returns a Java Duration, but we want a
        // Scala duration.
        //
        // TODO: When we upgrade to Scala 2.13, we can use the built-in converters
        // rather than going via the number of nanoseconds.
        // See https://stackoverflow.com/a/55412743/1558022
        val nanos = underlying.getDuration(s).toNanos
        Duration.fromNanos(nanos)
      }

    def requireDuration(path: String): Duration =
      getDurationOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }
  }
}
