val projectVersion = "25.1.0"

lazy val fixtures = Common.setupProject(
  project,
  "fixtures",
  projectVersion,
  externalDependencies = Dependencies.fixturesDependencies
)

lazy val json = Common.setupProject(
  project,
  "json",
  projectVersion,
  externalDependencies = Dependencies.jsonDependencies
)

lazy val typesafe_app = Common.setupProject(
  project,
  "typesafe_app",
  projectVersion,
  localDependencies = Seq(fixtures),
  externalDependencies = Dependencies.typesafeAppDependencies
)

lazy val monitoring = Common.setupProject(
  project,
  "monitoring",
  projectVersion,
  localDependencies = Seq(typesafe_app, fixtures),
  externalDependencies = Dependencies.monitoringDependencies
)

lazy val monitoring_typesafe = Common.setupProject(
  project,
  "monitoring_typesafe",
  projectVersion,
  localDependencies = Seq(monitoring)
)

lazy val storage = Common.setupProject(
  project,
  "storage",
  projectVersion,
  localDependencies = Seq(typesafe_app, fixtures, json),
  externalDependencies = Dependencies.storageDependencies
)

lazy val storage_typesafe = Common.setupProject(
  project,
  "storage_typesafe",
  projectVersion,
  localDependencies = Seq(storage)
)

lazy val messaging = Common.setupProject(
  project,
  "messaging",
  projectVersion,
  localDependencies = Seq(typesafe_app, fixtures, json, monitoring),
  externalDependencies = Dependencies.messagingDependencies
)

lazy val messaging_typesafe = Common.setupProject(
  project,
  "messaging_typesafe",
  projectVersion,
  localDependencies = Seq(messaging, monitoring_typesafe)
)

/**
 * To release to the Wellcome maven repo in S3 locally, uncomment this:
 **/
//import java.util.UUID
//import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
//s3CredentialsProvider := { _ =>
//  val builder = new STSAssumeRoleSessionCredentialsProvider.Builder(
//    "arn:aws:iam::760097843905:role/platform-developer",
//    UUID.randomUUID().toString
//  )
//  builder.build()
//}