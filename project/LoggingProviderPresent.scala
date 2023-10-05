import sbt.Keys._
import sbt._

/**
 * Since version 2, SLF4J requires the presence of a compatible logging provider on the classPath
 * (see https://www.slf4j.org/codes.html#ignoredBindings)
 *
 * This constraint may be satisfied by various libraries, but in our case, this is always logback >=1.3
 *
 */
trait LoggingProviderPresent {
  lazy val checkSLF4JProvider = taskKey[Unit]("If SLF4J 2 is present, check if it has a provider")

  lazy val checkSLF4JProviderImpl= Def.task  {
    val jars = update.value.matching(
      artifactFilter(`type` = "jar") && configurationFilter(name = "compile") &&
        (artifactFilter(name = "slf4j-api") || artifactFilter(name = "logback-classic"))

    ).map(_.name)
    jars.find(_.startsWith("slf4j-api-2")) match {
      case None =>
      case Some(_) => jars.find(_.startsWith("logback-classic")) match {
        case Some(logbackJar) if logbackJar.stripPrefix("logback-classic-").stripSuffix(".jar") >= "1.3" =>
        case _ =>
          streams.value.log.warn(s"slf4j 2 is present without a compatible logback provider in ${projectID.value}")
      }
    }
  }


}
