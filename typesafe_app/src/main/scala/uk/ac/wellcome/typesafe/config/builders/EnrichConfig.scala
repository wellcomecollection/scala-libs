package uk.ac.wellcome.typesafe.config.builders

import com.typesafe.config.Config

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
      getPathValue(path) { cleanPath =>
        underlying.getString(cleanPath)
      }

    def requireString(path: String): String =
      getStringOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }

    def getIntOption(path: String): Option[Int] =
      getPathValue(path) { cleanPath =>
        underlying.getInt(cleanPath)
      }

    def requireInt(path: String): Int =
      getIntOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }
  }
}
