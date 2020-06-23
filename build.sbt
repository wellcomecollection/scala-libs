lazy val fixtures = Common.setupProject(
  project,
  "fixtures",
  externalDependencies = Dependencies.fixturesDependencies
)

lazy val json = Common.setupProject(
  project,
  "json",
  externalDependencies = Dependencies.jsonDependencies
)

lazy val typesafe_app = Common.setupProject(
  project,
  "typesafe_app",
  localDependencies = Seq(fixtures),
  externalDependencies = Dependencies.typesafeAppDependencies
)

lazy val monitoring = Common.setupProject(
  project,
  "monitoring",
  localDependencies = Seq(typesafe_app, fixtures),
  externalDependencies = Dependencies.monitoringDependencies
)

lazy val monitoring_typesafe = Common.setupProject(
  project,
  "monitoring_typesafe",
  localDependencies = Seq(monitoring)
)

lazy val storage = Common.setupProject(
  project,
  "storage",
  localDependencies = Seq(typesafe_app, fixtures, json),
  externalDependencies = Dependencies.storageDependencies
)

lazy val storage_typesafe = Common.setupProject(
  project,
  "storage_typesafe",
  localDependencies = Seq(storage)
)

lazy val messaging = Common.setupProject(
  project,
  "messaging",
  localDependencies = Seq(typesafe_app, fixtures, json, monitoring),
  externalDependencies = Dependencies.messagingDependencies
)

lazy val messaging_typesafe = Common.setupProject(
  project,
  "messaging_typesafe",
  localDependencies = Seq(messaging, monitoring_typesafe)
)

/**
 * To release to the Wellcome maven repo in S3 locally, uncomment this:
 **/
//import java.util.UUID
//import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
//s3CredentialsProvider := { _ =>
//  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
//    "arn:aws:iam::760097843905:role/platform-dev",
//    UUID.randomUUID().toString
//  )
//  builder.build()
//}