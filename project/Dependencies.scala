import sbt._

object Dependencies {
  lazy val versions = new {
    val akka = "2.6.9"
    val akkaStreamAlpakka = "1.1.2"
    val elasticApm = "1.12.0"

    // Must match version used by Scanamo
    val aws = "1.11.504"

    // Moving what we can to version 2 of the AWS SDKs
    val aws2 = "2.11.14"

    val azure = "12.7.0"

    val circe = "0.13.0"
    val typesafe = "1.3.2"
    val logback = "1.1.8"
    val mockito = "1.10.19"
    val scalatest = "3.2.3"
    val scalatestPlus = "3.1.2.0"
    val scalatestPlusMockitoArtifactId = "mockito-3-2"
    val scanamo = "1.0.0-M10"
    val apacheCommons = "2.6"

    // Provides slf4j-api
    val grizzled = "1.3.2"
  }

  val circeDependencies = Seq(
    "io.circe" %% "circe-core" % versions.circe,
    "io.circe" %% "circe-generic" % versions.circe,
    "io.circe" %% "circe-generic-extras" % versions.circe,
    "io.circe" %% "circe-parser" % versions.circe
  )

  val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % versions.scalatest % Test,
    "org.scalatestplus" %% versions.scalatestPlusMockitoArtifactId % versions.scalatestPlus % Test,
    "org.mockito" % "mockito-core" % versions.mockito % Test
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

  val akkaDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akka,
    "com.typesafe.akka" %% "akka-stream" % versions.akka,
    // Force Akka to use SL4J logging adapter
    // https://doc.akka.io/docs/akka/current/logging.html#slf4j
    "com.typesafe.akka" %% "akka-slf4j" % versions.akka
  )

  val apacheCommons = Seq(
    "commons-io" % "commons-io" % versions.apacheCommons,
    "commons-io" % "commons-io" % versions.apacheCommons % "test"
  )

  val scanamoDependencies = Seq(
    "org.scanamo" %% "scanamo" % versions.scanamo,
    "org.scanamo" %% "scanamo-time" % versions.scanamo
  )

  val openTracingDependencies = Seq(
    "io.opentracing.contrib" %% "opentracing-scala-concurrent" % "0.0.6",
    "io.opentracing" % "opentracing-mock" % "0.33.0" % Test
  )

  val elasticApmBridgeDependencies = Seq(
    "co.elastic.apm" % "apm-opentracing" % versions.elasticApm,
    "co.elastic.apm" % "apm-agent-attach" % versions.elasticApm
  )

  val monitoringDependencies = Seq(
    "software.amazon.awssdk" % "cloudwatch" % versions.aws2
  ) ++
    testDependencies

  val messagingDependencies = Seq(
    "software.amazon.awssdk" % "sns" % versions.aws2,
    "software.amazon.awssdk" % "sqs" % versions.aws2,
    "com.lightbend.akka" %% "akka-stream-alpakka-sqs" % versions.akkaStreamAlpakka
    // This needs to be excluded because it conflicts with aws http client "netty-nio-client"
    // and it also causes weird leaks between tests
      exclude ("com.github.matsluni", "aws-spi-akka-http_2.12"),
    "io.circe" %% "circe-yaml" % versions.circe
  ) ++
    openTracingDependencies ++
    elasticApmBridgeDependencies ++
    testDependencies

  val jsonDependencies =
    circeDependencies ++
      sl4jDependencies ++
      testDependencies

  val fixturesDependencies =
    sl4jDependencies

  val typesafeAppDependencies =
    akkaDependencies ++
      loggingDependencies ++
      typesafeDependencies ++
      testDependencies

  val storageDependencies: Seq[ModuleID] = Seq(
    "com.azure" % "azure-storage-blob" % versions.azure,
    "com.amazonaws" % "aws-java-sdk-dynamodb" % versions.aws,
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws
  ) ++
    scanamoDependencies ++
    apacheCommons
}
