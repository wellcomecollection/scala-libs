import java.io.File

import com.tapad.docker.DockerComposePlugin
import sbt.Keys._
import sbt._

object Common {
  val projectVersion = "13.0.0"

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

  def setupProject(
                    project: Project,
                    folder: String,
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

    project
      .in(new File(folder))
      .settings(Common.settings: _*)
      .settings(DockerCompose.settings: _*)
      .enablePlugins(DockerComposePlugin)
      .dependsOn(dependsOn: _*)
      .settings(libraryDependencies ++= externalDependencies)
  }
}

