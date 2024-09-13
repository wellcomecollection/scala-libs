import com.jsuereth.sbtpgp.PgpKeys.*

import java.io.File
import com.tapad.docker.DockerComposePlugin
import sbt.Keys.*
import sbt.*
import xerial.sbt.Sonatype.autoImport.{sonatypeCredentialHost, sonatypePublishToBundle, sonatypeRepository}

object Common {
  def createSettings(projectVersion: String): Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.12.15",
    organization := "org.wellcomecollection",
    homepage := Some(url("https://github.com/wellcomecollection/scala-libs")),
    scmInfo := Some(
        ScmInfo(
          url("https://github.com/wellcomecollection/scala-libs"),
          "scm:git:git@github.com:wellcomecollection/scala-libs.git"
        )
    ),
    developers ++= List(
        Developer(
          id    = "sbrychta",
          name  = "Stepan Brychta",
          email = "s.brychta@wellcome.org",
          url   = url("https://github.com/StepanBrychta")
        )
    ),
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xlint",
      "-Xverify",
      "-Xfatal-warnings",
      "-Ypartial-unification",
      "-feature",
      "-language:postfixOps"
    ),
    parallelExecution in Test := false,
    publishMavenStyle := true,
    credentials += Credentials(Path.userHome / ".sbt" / "sonatype.credentials"),
//    pgpPublicRing := file(System.getProperty("user.home")) / ".gnupg" / "pubring.kbx",
//    pgpSecretRing := file(System.getProperty("user.home")) / ".gnupg" / "secring.gpg",
    pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray),
    sonatypeCredentialHost := "central.sonatype.com",
    sonatypeRepository := "https://central.sonatype.com/service/local",
    licenses := Seq("MIT" -> url("https://github.com/wellcomecollection/scala-libs/blob/main/LICENSE")),
    publishTo := sonatypePublishToBundle.value,
    publishArtifact in Test := true,
    // Don't build scaladocs
    // https://www.scala-sbt.org/sbt-native-packager/formats/universal.html#skip-packagedoc-task-on-stage
    mappings in (Compile, packageDoc) := Nil,
    version := projectVersion
  )

  def setupProject(
    project: Project,
    folder: String,
    projectVersion: String,
    localDependencies: Seq[Project] = Seq(),
    externalDependencies: Seq[ModuleID] = Seq()
  ): Project = {
    val dependsOn = localDependencies
      .map { project: Project =>
        ClasspathDependency(
          project = project,
          configuration = Some("compile->compile;test->test")
        )
      }

    val settings = createSettings(projectVersion)

    project
      .in(new File(folder))
      .settings(settings: _*)
      .settings(DockerCompose.settings: _*)
      .enablePlugins(DockerComposePlugin)
      .dependsOn(dependsOn: _*)
      .settings(libraryDependencies ++= externalDependencies)
  }
}

