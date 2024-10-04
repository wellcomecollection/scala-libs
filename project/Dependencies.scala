import sbt._

object Dependencies {
  lazy val versions = new {
    val elasticApm = "1.52.0"
    val elastic4s = "8.11.5"

    val aws = "2.25.70"

    // Note: this should probably match the version of Circe used by elastic4s.
    // See https://github.com/sksamuel/elastic4s/blob/master/project/Dependencies.scala
    val circe = "0.14.10"
    val circeGenericExtras = "0.14.4"

    val typesafe = "1.4.3"
    val logback = "1.5.8"
    val mockito = "5.14.1"
    val scalatest = "3.2.19"
    val scalatestPlus = "3.1.2.0"
    val scalatestPlusMockitoArtifactId = "mockito-3-2"
    val scanamo = "1.1.1"
    val apacheCommons = "2.17.0"

    // Provides slf4j-api
    val grizzled = "1.3.4"

    val pekko = "1.1.1"
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
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-streams-pekko" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
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
