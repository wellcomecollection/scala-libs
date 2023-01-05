val projectVersion = "32.19.1"

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

lazy val elasticsearch = Common.setupProject(
  project,
  "elasticsearch",
  projectVersion,
  localDependencies = Seq(fixtures, json),
  externalDependencies = Dependencies.elasticsearchDependencies
)

lazy val elasticsearch_typesafe = Common.setupProject(
  project,
  "elasticsearch_typesafe",
  projectVersion,
  localDependencies = Seq(elasticsearch, typesafe_app)
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

lazy val http = Common.setupProject(
  project,
  "http",
  projectVersion,
  localDependencies = Seq(json, monitoring),
  externalDependencies = Dependencies.httpDependencies
)

lazy val http_typesafe = Common.setupProject(
  project,
  "http_typesafe",
  projectVersion,
  localDependencies = Seq(http)
)

lazy val sierra = Common.setupProject(
  project,
  folder = "sierra",
  projectVersion = projectVersion,
  localDependencies = Seq(fixtures, http, json)
)

lazy val sierra_typesafe = Common.setupProject(
  project,
  folder = "sierra_typesafe",
  projectVersion = projectVersion,
  localDependencies = Seq(sierra, typesafe_app)
)
