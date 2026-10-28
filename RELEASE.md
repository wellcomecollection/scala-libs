RELEASE_TYPE: minor

Move elastic4s from `com.sksamuel.elastic4s` 8.11.5 to `nl.gn0s1s` 8.19.2, the project's new group and its last release for Scala 2.12. This brings jackson 2.22.2, circe 0.14.16 and pekko 1.7.0, which downstream repos pick up through this library. The elasticsearch tests now run against Elasticsearch 9.5.5, the version of the catalogue cluster.
