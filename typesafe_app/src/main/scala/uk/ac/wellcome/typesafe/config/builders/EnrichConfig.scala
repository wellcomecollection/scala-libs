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

    def getStringOption(path: String): Option[String] = {
      getPathValue(path) { cleanPath =>
        underlying.getString(cleanPath)
      }
    }

    def requireString(path: String): String = {
      getStringOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }
    }

    def getIntOption(path: String): Option[Int] = {
      getPathValue(path) { cleanPath =>
        underlying.getInt(cleanPath)
      }
    }

    def requireInt(path: String): Int = {
      getIntOption(path) getOrElse {
        emergencyStop(path)
        throw new Throwable(
          s"No value found for path ${cleanUpPath(path)}"
        )
      }
    }

    @deprecated(
      message =
        "This method may not work as expected and will be removed in a coming release!",
      since = "19 Jun 2020"
    )
    def get[T](path: String): Option[T] =
      if (underlying.hasPath(cleanUpPath(path))) {
        Some(underlying.getAnyRef(cleanUpPath(path)).asInstanceOf[T])
      } else {
        None
      }

    @deprecated(
      message =
        "This method may not work as expected and will be removed in a coming release!",
      since = "19 Jun 2020"
    ) def required[T](path: String): T =
      get(path).getOrElse {

        // For some reason merely throwing an exception here doesn't cause the
        // app to exit, so a config failure just sits.  This causes a config
        // error to crash the app.
        println(s"No value found for path ${cleanUpPath(path)}")
        System.exit(1)
        throw new RuntimeException(
          s"No value found for path ${cleanUpPath(path)}")
      }

    @deprecated(
      message =
        "This method may not work as expected and will be removed in a coming release!",
      since = "19 Jun 2020"
    ) def getOrElse[T](path: String)(default: T): T =
      get(path).getOrElse(default)
  }
}
