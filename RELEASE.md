RELEASE_TYPE: minor

Move elastic4s from `com.sksamuel.elastic4s` 8.11.5 to `nl.gn0s1s` 8.19.0, the project's new group. This brings pekko 1.1.5 and circe 0.14.14, which downstream repos pick up through this library; repos that pin pekko to match scala-libs need to move to 1.1.5. The elasticsearch tests now run against Elasticsearch 9.5.5, the version of the catalogue cluster.
