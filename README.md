# scala-libs

[![Build status](https://badge.buildkite.com/4a84a28feca6865e192e0adaba1c2e33f1e773e58957459c47.svg?branch=main)](https://buildkite.com/wellcomecollection/scala-libraries)



A collection of scala libraries used at the Wellcome Collection.

Includes:
- [fixtures](fixtures/README.md): Some shared test helpers and fixtures.
- [json](json/README.md):
Common JSON serialisation & de-serialisation.
- [messaging](messaging/README.md): Messaging libraries for inter-service communication.
- [monitoring](monitoring/README.md): Shared monitoring libraries in use by services
- [storage](monitoring/README.md): For working with storage providers such as DynamoDB and S3.
- [typesafe_app](typesafe_app/README.md): Used as a base for our Scala applications that use Typesafe.

## Installation

This libraries are only published to a private S3 bucket.

Wellcome projects have access to this S3 bucket -- you can use our build
scripts to publish a copy to your own private package repository, or vendor
the library by copying the code into your own repository.

Read [the changelog](CHANGELOG.md) to find the latest version.

```scala
libraryDependencies ++= Seq(
  "weco" %% "json" % "10.0.2",
  "weco" %% "messaging" % "10.0.2",
  "weco" %% "monitoring" % "10.0.2"
  "weco" %% "storage" % "10.0.2"
  "weco" %% "typesafe_app" % "10.0.2"
)
```

## Releasing

To release from this repository, please create a pull request with a `RELEASE.md` in the root directory in the following format:

```md
RELEASE_TYPE: major|minor|patch

### Libraries affected

`first_lib`, `second_lib`

### Description

A detailed description of the chnages in this release
```

When merged the version will be auto-incremented in line with the [SEMVER](https://semver.org/) standard.
