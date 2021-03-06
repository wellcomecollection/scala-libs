# CHANGELOG

**NOTE: This should not be updated, it contains the changelog for the library before it was moved to a unified repo.**

## v9.3.0 - 2020-05-07

This release changes how some of the SQS helpers work.

*   `withLocalSqsQueueAndDlq` and `withLocalSqsQueueAndDlqAndTimeout` are both renamed to `withLocalSqsQueuePair()`, which takes an optional `visibilityTimeout` argument.

    The DLQ name will also be the name of the main queue suffixed with `-dlq` for easier debugging.

*   `withLocalSqsQueue` now takes an optional `visibilityTimeout` argument.

    If you are using it without arguments, e.g.:

    ```scala
    withLocalSqsQueue { queue =>
      // do stuff with queue
    }
    ```

    you need to add empty parentheses:

    ```scala
    withLocalSqsQueue() { queue =>
      // do stuff with queue
    }
    ```

*   The `SQS.Queue` case class now includes the `visibilityTimeout`.

*   The following helpers have been made private or removed, because they weren't in use in the catalogue or storage-service repos:

    -   `assertQueueNotEmpty()`
    -   `assertQueuePairSizes()`
    -   `waitVisibilityTimeoutExipiry()`

## v9.2.1 - 2020-05-06

Add async sqs client for localstack in SQS fixtures

## v9.2.0 - 2020-05-05

Add a new method `getQueueAttribute(queue: Queue, attributeName: QueueAttributeName)` to the SQS fixture, for retrieving the value of a queue attribute.

Improve the assertion messages when checking queue sizes in the SQS fixture.

## v9.1.0 - 2020-04-30

Upgrade circe to 0.13.0

## v9.0.1 - 2020-04-29

This patch tidies up the dependency tree of this library.  Specifically, it removes some explicit dependencies that are duplicated from other Wellcome Scala libraries (Circe, Akka, Logback).

## v9.0.0 - 2020-04-27

Upgrade akka, alpakka, aws sdk and scalatest

## v8.0.0 - 2020-04-27

Add `OpenTracingMonitoringProcessor` and further refactor `Worker` and `MonitoringProcessor` interfaces

## v7.0.0 - 2020-02-14

Refactor `Worker` and `MonitoringProcessor` interfaces to allow for monitoring context to be carried across different services in messages.

## v6.0.0 - 2020-02-12

Refactor `Worker` interface to allow for more flexibile monitoring

## v5.3.1 - 2019-06-03

Remove Mockito from the SQS trait and use MemoryMetrics everywhere, and bump the version of scala-monitoring.

## v5.3.0 - 2019-05-30

Remove all the hybrid S3/SNS messaging code from this repo, and move it to the catalogue repo.

This breaks the dependency between scala-messaging and scala-storage.

## v5.2.0 - 2019-05-29

Bump the version of scala-storage, simplify the constructor of `MemoryMessageSender`, and simplify the `SNS` fixture.

## v5.1.1 - 2019-05-28

Bump the version of scala-storage to 6.0.1

## v5.1.0 - 2019-05-21

This release adds a helper method to `MemoryMessageSender` to make it easier to see the messages it's received.

## v5.0.0 - 2019-05-21

Bump the version of scala-storage to 5.0.0.

## v4.4.0 - 2019-05-15

Bump the version of scala-storage to 4.6.0.

## v4.3.0 - 2019-05-15

Bump the version of scala-storage to v4.5.0.

## v4.2.0 - 2019-05-13

Bump the version of scala-storage to 4.4.0.

## v4.1.0 - 2019-05-03

Bump the versions of scala-storage and scala-monitoring.

## v4.0.0 - 2019-05-03

Adds an intermediate AkkaWorker type and further generalises Worker behaviour

## v3.0.0 - 2019-05-02

Bump the version of scala-storage to 4.1.0.

## v2.0.0 - 2019-05-02

Bump the version of scala-storage to 4.0.0.

## v1.12.0 - 2019-04-26

Bump the version of scala-storage.

## v1.11.2 - 2019-04-24

Bump the version of scala-storage.

## v1.11.1 - 2019-04-24

Bump the version of scala-storage.

## v1.11.0 - 2019-04-16

Bump the version of scala-storage.

## v1.10.1 - 2019-04-05

Bump the version of scala-storage.

## v1.10.0 - 2019-04-04

Improvements to ergonomics of the new AlpakkaSQSWorker-related fixtures.

## v1.9.0 - 2019-04-02

add a cloudwatch implementation for the monitoring client

## v1.8.0 - 2019-03-26

Simplify worker

## v1.7.0 - 2019-03-26

Add a "worker" imlpementation.

## v1.6.0 - 2019-03-12

Some internal refactoring to use our new scala-monitoring library.

## v1.5.0 - 2019-02-25

This release removes the need to pass an `s3Client` to `SQS.createHybridRecordWith`
and `SQS.createHybridRecordNotificationWith`.

## v1.4.0 - 2019-02-25

This release adds a `namespace` parameter to `SNSBuilder.buildSNSWriter`.

## v1.3.0 - 2019-02-22

This release adds a new class, `NotificationStream[T]`, which does the work of
wrapping an `SQSStream[NotificationMessage]`, reading those messages from the
queue and unpacking them as instances of the case class `T` – hiding the mucky
JSON decoding from the worker services, which just get handed lovely case classes.

## v1.2.0 - 2019-02-08

This release adds the `messaging_typesafe` library for configuring the `messaging` library using Typesafe.

## v1.1.2 - 2019-02-06

Start using the scala-fixtures lib rather than vendoring fixtures.

## v1.1.1 - 2019-02-06

This fixes a small flakiness in `SNS.notificationMessage[T](topic)`, where it
would occasionally fail an assert if a duplicate message was sent twice.

## v1.1.0 - 2019-01-10

Bump the version of scala-monitoring to 1.2.0.

## v1.0.0 - 2018-12-10

First release of the messaging code in the platform repo.

## v0.2.0 - 2018-12-08

Bump the version of scala-storage to 3.1.0.

## v0.1.0 - 2018-12-07

Bump the version of scala-json to 1.1.1.

## v0.0.3 - 2018-12-07

Internal refactoring to simplify the SQS fixtures.

## v0.0.2 - 2018-12-07

Rename the `withActorSystem` method used internally for scala-messaging tests.

## v0.0.1 - 2018-12-07

Initial release of scala-messaging!
