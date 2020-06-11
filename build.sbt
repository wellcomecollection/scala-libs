import java.io.File
import java.util.UUID

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider

val projectName = "libs"
val projectVersion = "1.0.0"

enablePlugins(DockerComposePlugin)

// Everything below this line is generic boilerplate that should be reusable,
// unmodified, in all of our Scala libraries that have a "core" and a "typesafe"
// version.

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
  resolvers ++= Seq(
    "S3 releases" at "s3://releases.mvn-repo.wellcomecollection.org/"
  ),
  publishMavenStyle := true,
  publishTo := Some(
    "S3 releases" at "s3://releases.mvn-repo.wellcomecollection.org/"
  ),
  publishArtifact in Test := true,
  version := projectVersion
)

// Temporarily commented out until https://github.com/wellcometrust/platform/issues/3806
// In order to access our libraries in S3 we need to set the following:

s3CredentialsProvider := { _ =>
  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
    "arn:aws:iam::760097843905:role/platform-read_only",
    UUID.randomUUID().toString
  )

  builder.build()
}

lazy val typesafe_app =
  project
    .withId("typesafe_app")
    .in(new File("typesafe_app"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.typesafeAppDependencies)


lazy val storage =
  project
    .withId("storage")
    .in(new File("storage"))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.storageDependencies)
    .dependsOn(typesafe_app % "compile->compile;test->test")

lazy val storage_typesafe =
  project
    .withId(s"storage_typesafe")
    .in(new File(s"storage_typesafe"))
    .settings(settings)
    .dependsOn(storage % "compile->compile;test->test")

lazy val root = (project in file("."))
  .withId("scala-libs")
  .aggregate(typesafe_app, storage, storage_typesafe)
  .settings(
    Seq(
      // We don't want to publish the aggregate project, just the sub projects.
      // See https://stackoverflow.com/a/46986683/1558022
      skip in publish := true
    ))
