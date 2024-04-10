import sbt._

object Dependencies {
  lazy val versions = new {
    val elasticApm = "1.22.0"
    val elastic4s = "8.8.1"

    val aws = "2.25.28"

    // Note: this should probably match the version of Circe used by elastic4s.
    // See https://github.com/sksamuel/elastic4s/blob/master/project/Dependencies.scala
    val circe = "0.14.5"
    val circeGenericExtras = "0.14.3"

    val typesafe = "1.4.2"
    val logback = "1.4.7"
    val mockito = "1.10.19"
    val scalatest = "3.2.3"
    val scalatestPlus = "3.1.2.0"
    val scalatestPlusMockitoArtifactId = "mockito-3-2"
    val scanamo = "1.0-M13"
    val apacheCommons = "2.6"

    // Provides slf4j-api
    val grizzled = "1.3.4"

    // This has to match the version of akka used by elastic4s
    // Otherwise we get errors like:
    //
    //      An exception or error caused a run to abort: You are using version 2.6.14 of Akka,
    //      but it appears you (perhaps indirectly) also depend on older versions of related
    //      artifacts. You can solve this by adding an explicit dependency on version 2.6.14
    //      of the [akka-slf4j, akka-stream, akka-testkit] artifacts to your project.
    //
    // See https://github.com/sksamuel/elastic4s/blob/master/project/Dependencies.scala
    //
    val akka = "2.6.20"
    val akkaStreamAlpakka = "3.0.4"

    // Getting the akka-http dependencies right can be fiddly and takes some work.
    // In particular you need to use the same version of akka-http everywhere, or you
    // get errors (from LargeResponsesTest) like:
    //
    //      Detected possible incompatible versions on the classpath. Please note that
    //      a given Akka HTTP version MUST be the same across all modules of Akka HTTP
    //      that you are using, e.g. if you use [10.1.10] all other modules that are
    //      released together MUST be of the same version.
    //
    //      Make sure you're using a compatible set of libraries.
    //
    // To work this out:
    //
    //   1. Look at the version of alpakka-streams used above.
    //
    //   2. Look at the corresponding akka-http dependency in alpakka:
    //      https://github.com/akka/alpakka/blob/master/project/Dependencies.scala
    //      (At time of writing, alpakka v3.0.1 pulls in akka-http 10.1.11)
    //
    val akkaHttp = "10.2.9"

    // This needs to be set explicitly to match the language version
    // used by the version of shapeless that Circe uses, otherwise SBT
    // is liable to resolve it incorrectly when packaging applications.
    // Without doing this, you might come across weird errors like:
    //
    //    java.lang.NoClassDefFoundError: scala/reflect/internal/Names$Name
    //
    val scalaReflectVersion = "2.12.15"
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
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % versions.elastic4s,
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

  val akkaDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-stream" % versions.akka,
    // Force Akka to use SL4J logging adapter
    // https://doc.akka.io/docs/akka/current/logging.html#slf4j
    "com.typesafe.akka" %% "akka-slf4j" % versions.akka
  )

  val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http" % versions.akkaHttp
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
    "net.java.dev.jna" % "jna" % "5.7.0"
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
    "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % versions.akkaStreamAlpakka
    // This needs to be excluded because it conflicts with aws http client "netty-nio-client"
    // and it also causes weird leaks between tests
      exclude("com.github.matsluni", "aws-spi-akka-http_2.12")
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
    akkaDependencies ++
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
    akkaHttpDependencies
}
