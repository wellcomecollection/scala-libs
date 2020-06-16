import java.io.File
import java.util.UUID

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider

val projectName = "libs"
val projectVersion = "10.0.4"

enablePlugins(DockerComposePlugin)

val settings: Seq[Def.Setting[_]] = Seq(
  scalaVersion := "2.12.6",
  organization := "uk.ac.wellcome",
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
  publishTo := Some(
    "S3 releases" at "s3://releases.mvn-repo.wellcomecollection.org/"
  ),
  publishArtifact in Test := true,
  version := projectVersion
)

lazy val fixtures =
  project
    .withId("fixtures")
    .in(new File("fixtures"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.fixturesDependencies)

lazy val json =
  project
    .withId("json")
    .in(new File("json"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.jsonDependencies)

lazy val typesafe_app =
  project
    .withId("typesafe_app")
    .in(new File("typesafe_app"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.typesafeAppDependencies)
    .dependsOn(fixtures % "compile->compile;test->test")

lazy val monitoring =
  project
    .withId("monitoring")
    .in(new File("monitoring"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.monitoringDependencies)
    .dependsOn(typesafe_app % "compile->compile;test->test")
    .dependsOn(fixtures % "compile->compile;test->test")

lazy val monitoring_typesafe =
  project
    .withId(s"monitoring_typesafe")
    .in(new File(s"monitoring_typesafe"))
    .settings(settings)
    .dependsOn(monitoring % "compile->compile;test->test")

lazy val storage =
  project
    .withId("storage")
    .in(new File("storage"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.storageDependencies)
    .dependsOn(typesafe_app % "compile->compile;test->test")
    .dependsOn(fixtures % "compile->compile;test->test")
    .dependsOn(json % "compile->compile;test->test")

lazy val storage_typesafe =
  project
    .withId(s"storage_typesafe")
    .in(new File(s"storage_typesafe"))
    .settings(settings)
    .dependsOn(storage % "compile->compile;test->test")

lazy val messaging =
  project
    .withId("messaging")
    .in(new File("messaging"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.messagingDependencies)
    .dependsOn(monitoring % "compile->compile;test->test")
    .dependsOn(typesafe_app % "compile->compile;test->test")
    .dependsOn(fixtures % "compile->compile;test->test")
    .dependsOn(json % "compile->compile;test->test")

lazy val messaging_typesafe =
  project
    .withId(s"messaging_typesafe")
    .in(new File(s"messaging_typesafe"))
    .settings(settings)
    .dependsOn(messaging % "compile->compile;test->test")
    .dependsOn(monitoring_typesafe % "compile->compile;test->test")

lazy val root = (project in file("."))
  .withId("scala-libs")
  .aggregate(fixtures, json, typesafe_app, storage, storage_typesafe, messaging, messaging_typesafe, monitoring, monitoring_typesafe)
  .settings(
    Seq(
      // We don't want to publish the aggregate project, just the sub projects.
      // See https://stackoverflow.com/a/46986683/1558022
      skip in publish := true
    ))
