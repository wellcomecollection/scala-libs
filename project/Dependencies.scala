import sbt._

object Dependencies {
  lazy val versions = new {
    val elasticApm = "1.52.0"
    // nl.gn0s1s publishes elastic4s 9.x only for Scala 2.13 and 3; 8.19.2 is the last for 2.12
    val elastic4s = "8.19.2"

    val aws = "2.55.0"

    // Matches the jackson that elastic4s brings in
    val jackson = "2.22.2"

    // Note: this should probably match the version of Circe used by elastic4s.
    // See https://github.com/sksamuel/elastic4s/blob/master/project/Dependencies.scala
    val circe = "0.14.16"
    val circeGenericExtras = "0.14.4"

    val typesafe = "1.4.3"
    val logback = "1.5.8"
    val mockito = "5.14.1"
    val scalatest = "3.2.19"
    val scalatestPlus = "3.1.2.0"
    val scalatestPlusMockitoArtifactId = "mockito-3-2"
    val scanamo = "2.0.0"
    val apacheCommons = "2.17.0"

    // Provides slf4j-api
    val grizzled = "1.3.4"

    // elastic4s-streams-pekko brings pekko-stream 1.7.0, and pekko modules must share a version
    val pekko = "1.7.0"
    val pekkoConnectors = "1.0.2"
    val pekkoHttp = "1.1.0"
    val pekkoHttpJson = "2.8.0"

    // This needs to be set explicitly to match the language version
    // used by the version of shapeless that Circe uses, otherwise SBT
    // is liable to resolve it incorrectly when packaging applications.
    // Without doing this, you might come across weird errors like:
    //
    //    java.lang.NoClassDefFoundError: scala/reflect/internal/Names$Name
    //
    val scalaReflectVersion = "2.12.20"
  }

  val circeDependencies = Seq(
    "io.circe" %% "circe-core" % versions.circe,
    "io.circe" %% "circe-generic" % versions.circe,
    "io.circe" %% "circe-generic-extras" % versions.circeGenericExtras,
    "io.circe" %% "circe-parser" % versions.circe,
    "org.scala-lang" % "scala-reflect" % versions.scalaReflectVersion
  )

  val elasticsearchDependencies = Seq(
    "nl.gn0s1s" %% "elastic4s-core" % versions.elastic4s,
    "nl.gn0s1s" %% "elastic4s-client-esjava" % versions.elastic4s,
    "nl.gn0s1s" %% "elastic4s-streams-pekko" % versions.elastic4s,
    "nl.gn0s1s" %% "elastic4s-json-circe" % versions.elastic4s,
    "nl.gn0s1s" %% "elastic4s-testkit" % versions.elastic4s % "test",
    "com.fasterxml.jackson.core" % "jackson-core" % versions.jackson,
    "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson
  )

  val sl4jDependencies = Seq(
    "org.clapper" %% "grizzled-slf4j" % versions.grizzled
  )

  val loggingDependencies = Seq(
    "ch.qos.logback" % "logback-classic" % versions.logback,
    "ch.qos.logback" % "logback-core" % versions.logback,
    "ch.qos.logback" % "logback-access" % versions.logback
  ) ++ sl4jDependencies

  val typesafeDependencies: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % versions.typesafe
  )

  val scalatestDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % Test
  )

  val mockitoDependencies = Seq(
    "org.scalatestplus" %% versions.scalatestPlusMockitoArtifactId % versions.scalatestPlus % Test,
    "org.mockito" % "mockito-core" % versions.mockito % Test
  )

  val testDependencies: Seq[ModuleID] =
    scalatestDependencies ++
      mockitoDependencies ++
      // This needs to be here in order that Test output always works even for libraries that
      // don't include the logging dependencies.
      loggingDependencies.map(_ % Test)

  val pekkoDependencies: Seq[ModuleID] = Seq(
    "org.apache.pekko" %% "pekko-actor-typed" % versions.pekko,
    "org.apache.pekko" %% "pekko-actor-testkit-typed" % versions.pekko % Test,
    "org.apache.pekko" %% "pekko-stream" % versions.pekko,
    "org.apache.pekko" %% "pekko-slf4j" % versions.pekko,
  )


  val pekkoHttpDependencies = Seq(
    "org.apache.pekko" %% "pekko-http" % versions.pekkoHttp,
    "com.github.pjfanning" %% "pekko-http-circe" % versions.pekkoHttpJson
  )

  val apacheCommons = Seq(
    "commons-io" % "commons-io" % versions.apacheCommons,
    "commons-io" % "commons-io" % versions.apacheCommons % "test"
  )

  val scanamoDependencies = Seq(
    "org.scanamo" %% "scanamo" % versions.scanamo
  )

  val elasticApmAgentDependencies = Seq(
    "co.elastic.apm" % "apm-agent-attach" % versions.elasticApm
      // This needs to be excluded because it prevents this library functioning on M1 macs
      // at the current version of apm-agent-attach it pulls in version 5.3.1 of jna,
      // we need at least 5.7 to fix this issue. See https://github.com/java-native-access/jna/pull/1238
      exclude("net.java.dev.jna", "jna"),
    "co.elastic.apm" % "apm-agent-api" % versions.elasticApm,
  )

  // This is required to provide a version of jna to consumers of the apm library,
  // when running in a jre (as specified in Dockerfiles for deployable images).
  // See https://github.com/elastic/apm-agent-java/issues/2353 for an explanation
  // of the issue this addresses.
  val jnaDependencies: Seq[ModuleID] = Seq(
    "net.java.dev.jna" % "jna" % "5.15.0"
  )

  val localstackDependencies = Seq(
    "software.amazon.awssdk" % "auth" % versions.aws,
    "software.amazon.awssdk" % "regions" % versions.aws
  )

  val monitoringDependencies = Seq(
    "software.amazon.awssdk" % "cloudwatch" % versions.aws
  ) ++
    testDependencies

  val messagingDependencies = Seq(
    "software.amazon.awssdk" % "sns" % versions.aws,
    "software.amazon.awssdk" % "sqs" % versions.aws,
    "org.apache.pekko" %% "pekko-connectors-sqs" % versions.pekkoConnectors
  ) ++
    testDependencies

  val jsonDependencies =
    circeDependencies ++
      sl4jDependencies ++
      testDependencies

  val fixturesDependencies: Seq[ModuleID] =
    sl4jDependencies ++
      scalatestDependencies ++
      localstackDependencies

  val typesafeAppDependencies =
    pekkoDependencies ++
      loggingDependencies ++
      typesafeDependencies ++
      elasticApmAgentDependencies ++
      jnaDependencies ++
      testDependencies

  val storageDependencies: Seq[ModuleID] = Seq(
    "software.amazon.awssdk" % "s3" % versions.aws,
    "software.amazon.awssdk" % "dynamodb" % versions.aws
  ) ++
    scanamoDependencies ++
    apacheCommons

  val httpDependencies: Seq[ModuleID] =
    pekkoHttpDependencies
}
