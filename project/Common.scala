import java.io.File

import com.tapad.docker.DockerComposePlugin
import sbt.Keys._
import sbt._

object Common {
  def createSettings(projectVersion: String): Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.13.14",
    organization := "weco",
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

