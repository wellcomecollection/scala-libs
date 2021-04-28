import sbt._

object Dependencies {
  lazy val versions = new {
    val akka = "2.6.10"
    val akkaStreamAlpakka = "1.1.2"
    val elasticApm = "1.12.0"

    val elastic4s = "7.12.1"

    val aws = "1.11.504"

    // Moving what we can to version 2 of the AWS SDKs
    val aws2 = "2.11.14"

    val azure = "12.7.0"

    val akkaHttpCirce = "1.32.0"
    val circe = "0.13.0"
    val typesafe = "1.3.2"
    val jackson = "2.12.3"
    val javaxWebServicesRestful = "2.1.1"
    val logback = "1.1.8"
    val mockito = "1.10.19"
    val scalatest = "3.2.3"
    val scalatestPlus = "3.1.2.0"
    val scalatestPlusMockitoArtifactId = "mockito-3-2"
    val scanamo = "1.0-M13"
    val swagger = "2.1.9"
    val apacheCommons = "2.6"

    // Provides slf4j-api
    val grizzled = "1.3.2"

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
    //      (At time of writing, alpakka v1.1.2 pulls in akka-http 10.1.10)
    //
    //   3. Look at versions of akka-http-json.  Browse the Git tags until you find
    //      one that uses the same version of akka-http and a compatible Circe:
    //      https://github.com/hseeberger/akka-http-json/blob/master/build.sbt
    //
    val akkaHttp = "10.1.11"
  }

  val circeDependencies = Seq(
    "io.circe" %% "circe-core" % versions.circe,
    "io.circe" %% "circe-generic" % versions.circe,
    "io.circe" %% "circe-generic-extras" % versions.circe,
    "io.circe" %% "circe-parser" % versions.circe
  )
  val elasticsearchDependencies = Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-json-circe" % versions.elastic4s,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % versions.elastic4s % "test"
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

  val akkaHttpDependencies = Seq(
    "com.typesafe.akka" %% "akka-http" % versions.akkaHttp,
    "de.heikoseeberger" %% "akka-http-circe" % versions.akkaHttpCirce
  )

  val apacheCommons = Seq(
    "commons-io" % "commons-io" % versions.apacheCommons,
    "commons-io" % "commons-io" % versions.apacheCommons % "test"
  )

  val scanamoDependencies = Seq(
    "org.scanamo" %% "scanamo" % versions.scanamo
  )

  val openTracingDependencies = Seq(
    "io.opentracing.contrib" %% "opentracing-scala-concurrent" % "0.0.6",
    "io.opentracing" % "opentracing-mock" % "0.33.0" % Test
  )

  val elasticApmBridgeDependencies = Seq(
    "co.elastic.apm" % "apm-opentracing" % versions.elasticApm,
    "co.elastic.apm" % "apm-agent-attach" % versions.elasticApm
  )

  val swaggerDependencies = Seq(
    "io.swagger.core.v3" % "swagger-core" % versions.swagger,
    "io.swagger.core.v3" % "swagger-annotations" % versions.swagger,
    "io.swagger.core.v3" % "swagger-models" % versions.swagger,
    "io.swagger.core.v3" % "swagger-integration" % versions.swagger,
    "io.swagger.core.v3" % "swagger-jaxrs2" % versions.swagger,
    "javax.ws.rs" % "javax.ws.rs-api" % versions.javaxWebServicesRestful,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson
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
    "software.amazon.awssdk" % "dynamodb" % versions.aws2,
    "com.amazonaws" % "aws-java-sdk-s3" % versions.aws
  ) ++
    scanamoDependencies ++
    apacheCommons

  val httpDependencies: Seq[ModuleID] =
    swaggerDependencies ++ akkaHttpDependencies
}
