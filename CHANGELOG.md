# CHANGELOG

## v26.18.4 - 2021-06-17

Tidy up the constructor for AkkaHttpClient.

## v26.18.3 - 2021-06-17

Another no-op change to trigger a release and test the auto-bumping.

## v26.18.2 - 2021-06-16

Provide a better error message when MemoryHttpClient runs out of prepared responses.

## v26.18.1 - 2021-06-16

A no-op change to trigger a release and test the auto-bumping.

## v26.18.0 - 2021-06-16

Add an HTTP typesafe builder for the SierraOauthHttpClient.

## v26.17.7 - 2021-06-14

Be a bit more tolerant of equality when comparing JSON entities in MemoryHttpClient.

## v26.17.6 - 2021-06-14

Provide a better error message if we can't get the access token in the Sierra OAuth HttpClient.

## v26.17.5 - 2021-06-14

Provide a better error message if you pass invalid JSON to HttpFixtures.createJsonHttpEntityWith().

## v26.17.4 - 2021-06-14

Don't hard-code a version of the Sierra API in SierraOauthHttpClient.

## v26.17.3 - 2021-06-11

Tweak AkkaHttpClient so it logs requests and a baseUri isn't required.

## v26.17.2 - 2021-06-10

Break up HttpClient so a `baseUri` isn't required unless you're using the GET and POST convenience helpers.

## v26.17.1 - 2021-06-10

Add some tests for SierraOauthHttpClient.

## v26.17.0 - 2021-06-10

Bump the version of Akka to be happy with the new version of elastic4s.

## v26.16.0 - 2021-06-10

This adds HTTP client-related classes from the catalogue-api repo.

## v26.15.0 - 2021-06-02

Bump elasticsearch and elastic4s to 7.12.2

## v26.14.0 - 2021-05-13

Add a TypedString type for times when we want to use a type in place of a string, but we still want the value to serialise as a string in JSON/DynamoDB.

We already have code for this in various places; this consolidates it into one place.

## v26.13.0 - 2021-05-13

This adds TimeAssertions to the fixtures library.

## v26.12.3 - 2021-05-07

Fix contextUrl leaking into fixture

## v26.12.2 - 2021-05-07

The elasticsearch_typesafe library now requires that you configure at least a host, username and password.

## v26.12.1 - 2021-05-07

Change assertDisplayError() so it doesn't expect `"description": null` in the JSON output if there is no description.

## v26.12.0 - 2021-05-06

Fix assertIsDisplayError

## v26.11.0 - 2021-05-06

This standardises the use of context URLs with a new trait:

```scala
trait HasContextUrl {
  def contextUrl: URL
}
```

## v26.10.1 - 2021-05-05



## v26.10.0 - 2021-05-05

Simplify error handling in http lib, add some tests for WellcomeHttpApp

## v26.9.2 - 2021-05-05

Make the `ElasticClient` in `ElasticsearchFixtures` an implicit value.

## v26.9.1 - 2021-04-30

Remember to publish http_typesafe

## v26.9.0 - 2021-04-30

Add WellcomeHttpApp

## v26.8.0 - 2021-04-29

Adds Tracing trait supporting APM tracing to typesafe_app lib, in order to make it available to all apps by default.

## v26.7.3 - 2021-04-27

Bump the version of akka-http to 10.1.11 (but actually correctly).

## v26.7.2 - 2021-04-27

Bump the version of akka-http to 2.6.11.

Tweak the behaviour of FutureDirectives.

## v26.7.1 - 2021-04-27

Make the printer on `DisplayJsonUtil` an implicit value.

## v26.7.0 - 2021-04-27

This release add the `ErrorDirectives` and `FutureDirectives` traits.
These are split out from the `CustomDirectives` traits that used to live in our application repos.

It also adds `akka-http-circe` as a dependency of the http lib.

## v26.6.0 - 2021-04-26

This release adds the shared `HttpMetrics` class.

## v26.5.0 - 2021-04-26

Add the first piece of a shared HTTP library, with some models from the catalogue API.

## v26.4.3 - 2021-03-22

This patch makes the locking service slightly more efficient in scenarios where multiple processes are trying to acquire overlapping sets of locks.

## v26.4.2 - 2021-03-22

SQSStream and AlpakkaSQSWorker are now using DeleteMessageBatch rather than DeleteMessage.
This should have no user-visible effects.

## v26.4.1 - 2021-03-16

Add possibility to update metadata supplied in an index mapping.

## v26.4.0 - 2021-03-11

Add `elasticsearch` and `elasticsearch_typesafe` modules

## v26.3.2 - 2021-02-17

If a service using `WellcomeApp` throws a terminal exception, it should actually stop running.
Currently, services hang around forever until they get purged by autoscaling.

## v26.3.1 - 2021-02-16

Adds a method to RandomGenerators for taking a random sample from a given Seq

## v26.3.0 - 2021-02-03

Adds the option to pass to SQSStream a function that creates an entire RunnableGraph from a source and sink.

## v26.2.1 - 2021-02-02

Add some tests to describe dynamo formatting behaviour

## v26.2.0 - 2021-01-20

Roll back the AWS SDK and other dependencies to known good versions.

## v26.1.0 - 2021-01-20

Bump the version of Alpakka Streams to 2.0.2.

## v26.0.2 - 2021-01-19

Fix a typo in an error log from SQSStream.

## v26.0.1 - 2021-01-19

Log failures from a MessageSender as an error.  This should make SNS failures more obvious.

## v26.0.0 - 2021-01-18

This release bumps the version of Scanamo to 1.0-M13, and the AWS SDK for DynamoDB to V2.

## v25.1.2 - 2021-01-14

This release changes DynamoLockDao to use BatchWriteItem to release locks.  This will have no impact on callers, but should make unlocking faster and cheaper if you lock multiple IDs.

## v25.1.1 - 2021-01-12

Remove a now-unused custom DynamoFormat for S3ObjectLocation.
We wrote this format when we changed the S3ObjectLocation model from namespace/path to bucket/key, to allow our Scala apps to read old-style locations.
We've now migrated our DynamoDB tables to use the new-style locations exclusively, so this translation code can be removed.

## v25.1.0 - 2021-01-05

### Libraries affected

`typesafe_app`

### Description

*   Remove some deprecated and now-unused methods on EnrichConfig
*   Add a new getBooleanOption method to EnrichConfig

## v25.0.4 - 2020-12-17

### Libraries affected

`messaging`

### Description

Log warning when receive throwable in SQSStream

## v25.0.3 - 2020-12-14

Expose the underlying stores in a VersionedHybridStore and a HybridStore as public attributes.

## v25.0.2 - 2020-12-14

Treat a `NativeIoException` when reading a blob from Azure as retryable.

## v25.0.1 - 2020-12-08

The error type of Maxima.max() should be MaximaError, not ReadError.

## v25.0.0 - 2020-12-08

This release changes the behaviour of Maxima to return an `Identified[_, _]`, rather than a single value.
This reduces doing double-lookups if you want to get the max version of something in a store, and retrieve the whole record rather than just the max version.

The practical upshot for our code is that if you call `getLatest(…)`, now you're making a single request to DynamoDB rather than two.

## v24.6.1 - 2020-12-02

Improve the efficiency of reading items from the Dynamo stores.

## v24.6.0 - 2020-11-24

Bump the version of akka to 2.6.10.

## v24.5.0 - 2020-11-24

Bump scalatest to 3.2.3 and scalatestplus-mockito to 3.1.2.0.

## v24.4.0 - 2020-11-23

Allow consistency to be set from DynamoSingleVersionStore, DynamoMultipleVersionStore

## v24.3.2 - 2020-11-20

Removes unused travis things

## v24.3.1 - 2020-11-20

Fix a flaky test in S3TagsTest.

## v24.3.0 - 2020-11-19

The `DynamoReadable` trait (and all extensions of it) now have a new field `consistencyMode`, which allows you to decide between:

*   **Eventually consistent reads**, which might not return data from a recent write operation
*   **Strongly consistent reads**, whcih always return the most recent data, but with higher latency and using more throughput capacity

The default is eventual consistency, which is the existing behaviour.

## v24.2.0 - 2020-11-19

Don't build scaladocs

## v24.1.1 - 2020-10-29

### Libraries affected

All (but no external changes)

### Description

Bump SBT version to 1.4.1

## v24.1.0 - 2020-10-26

Bump Akka to 2.6.9.  This is required for the latest version of elastic4s (7.9.1).

## v24.0.0 - 2020-10-22

### Libraries affected

messaging, monitoring

### Description

*   Metrics.recordValue() no longer takes an optional `metricUnit` parameter, because none of our code was using it.
*   MetricsMonitoringClient has been removed.  Any uses of it can be replaced by an instance of Metrics[Future].

## v23.0.0 - 2020-10-07

This release tidies up the random generators provided by `RandomGenerators`.  This trait should be the canonical source of random data for tests in the platform, rather than multiple implementations of very similar functions copy/pasted into different codebases.

## v22.0.0 - 2020-10-01

Add retry for 500 errors from S3 in `S3Transfer`

## v21.0.0 - 2020-09-24

Add a mandatory `signedUrlExpiry` parameter to `AzurePutBlockFromUrlTransfer` contructor that specifies how long the presigned URL from S3 should be valid for.

## v20.0.0 - 2020-09-22

Add some generic classes used in the storage-service:
* ByteRange and ByteRangeUtil
* SizeFinder
* RangedReader
* LargeStreamReader
* ObjectExists
* S3Uploader
* AzurePrefixTransfer and AzureTransfer

Delete classes that are not going to be used:
* AzureBlobMetadata
* S3ToAzurePrefixTransfer and S3ToAzureTransfer

## v19.6.1 - 2020-09-16

Add retry logic for TimeoutException in AzureStorageErrors

## v19.6.0 - 2020-09-08

### Libraries affected

`storage`

### Description

Allow creating an instance of `S3ObjectLocation` from an `S3ObjectSummary`.

## v19.5.6 - 2020-08-28

### Libraries affected

All

### Description

Bumps the AKKA version

## v19.5.5 - 2020-08-27

Add a defaults to `randomInt` in `fixtures` (also for buildkite testing)

## v19.5.4 - 2020-08-24

### Libraries affected

`storage`

### Description

Make the warning introduced in v19.5.3 a bit less chatty.  In particular, the Listing classes no longer warn if you try to list a prefix with an empty path -- i.e., the complete contents of an S3 bucket or Azure container.

## v19.5.3 - 2020-08-20

Warn that listing the contents of an S3/Azure prefix that doesn't end in a slash may include unexpected objects.

e.g. listing `bags/v1` will return everything under `bags/v1/`, but also `bags/v10/`.

This may be the desired behaviour, so listing a prefix that doesn't end with a slash isn't forbidden, but hopefully this makes it easier to debug if weird things are happening.

## v19.5.2 - 2020-08-19

### Libraries affected

`storage`

### Description

The `S3Tags` class will now retry writing tags to S3 if it receives a transient error from the SetObjectTagging API.

## v19.5.1 - 2020-08-19

### Libraries affected

`storage`

### Description

Internal refactoring and cleanups in RetryOps and RetryReadable.

## v19.5.0 - 2020-08-18

Remove the implicit DynamoFormat[ExpiringLock] parameter from the lock dao and instead provide it internally, therefore preventing inconsistent formatting.

## v19.4.4 - 2020-08-17

Remember to close streams that get opened in S3toAzureTransfer, otherwise you get the error:

> Unable to execute HTTP request: Timeout waiting for connection from pool

## v19.4.3 - 2020-08-10

Allow a trailing slash in an S3ObjectLocationPrefix and an AzureBlobLocationPrefix.

This also improves the error handling of illegal keys.

## v19.4.2 - 2020-08-06

Disallow using `.` and `..` in an S3ObjectLocation key and an AzureBlobLocation name.

In a filesystem, these entries mean "current directory" and "parent directory", but object stores like S3 and Azure Blob Storage aren't filesystems.
These characters cause issues in the console, and their semantics aren't the same as in regular filesystems.

e.g. on a filesystem, `alfa/./bravo` is equivalent to `alfa/bravo`, but these could be two distinct objects in S3

In general we never expect to be creating objects that include these entries in the path, so for now outright block them rather than trying to handle them.
**We can review this limitation if we need to support such keys, but we should consider the semantics carefully.**

## v19.4.1 - 2020-08-06

Add some tests that we remove double slashes when joining paths.

This isn't a change in behaviour, just a new test to avoid regressing this behaviour in future.

## v19.4.0 - 2020-08-06

### Libraries affected

`storage`

### Description

Add a constructor for `AzureTypedStore`, `S3toAzureTransfer` and `S3toAzurePrefixTransfer` that take implicit S3 and Azure clients, saving callers from constructing the intermediate classes.

## v19.3.1 - 2020-08-05

`send` on `MemoryIndividualMessageSender` now updates its internal message list using `synchronized` to prevent dropping entries with concurrent writes.

## v19.3.0 - 2020-07-29

### Libraries affected

`messaging`

### Description

Remove the `NotificationStream` class and the associated fixture and builders, which aren't used anywhere in the platform.

## v19.2.0 - 2020-07-29

Remove some unneeded Materializer instances:

In [Akka 2.6](https://doc.akka.io/docs/akka/current/project/migration-guide-2.5.x-2.6.x.html#materializer-changes), you don't need to supply an implicit Materializer if there's an implicit ActorSystem:

> A default materializer is now provided out of the box. For the Java API just pass system when running streams, for Scala an implicit materializer is provided if there is an implicit ActorSystem available. This avoids leaking materializers and simplifies most stream use cases somewhat.

The `withMaterializer(actorSystem: ActorSystem)` method has been removed from the `Akka` fixture; you can remove it anywhere it's used and rely on the implicit materializer.

## v19.1.0 - 2020-07-27

### Libraries affected

`storage`

### Description

This adds `basename`, `parent` and `asLocation` methods to `Prefix[_ <: Location]`.

## v19.0.0 - 2020-07-24

Replace the remaining uses of `ObjectLocation` with `S3ObjectLocation` or `MemoryLocation` throughout the storage library.

## v18.0.0 - 2020-07-24

Change the `S3Listing` and `S3Transfer` classes to use the new-style locations.

Replace all uses of `ObjectLocation` with `S3ObjectLocation` to get the new behaviour; the two classes are compatible.

## v17.0.0 - 2020-07-23

Change the `S3Tags` class to use the new-style locations.

Replace all uses of `ObjectLocation` with `S3ObjectLocation` to get the new behaviour; the two classes are compatible.

## v16.0.0 - 2020-07-23

Change all the Azure-related storage classes to use the new-style locations.

Replace all uses of `ObjectLocation` with `AzureBlobLocation` to get the new behaviour; the two classes are compatible.

## v15.0.0 - 2020-07-23

Changes to the `TransferResult` type hierarchy; refactoring in the Transfer and PrefixTransfer classes to make it easier to switch to new-style locations in the next patch.

## v14.2.0 - 2020-07-23

This release introduces four new classes in the **storage** library:

-   `S3ObjectLocation`
-   `S3ObjectLocationPrefix`
-   `AzureBlobLocation`
-   `AzureBlobLocationPrefix`

which extend new traits `Location` and `Prefix`, respectively.

These will eventually replace the existing `ObjectLocation` class, but `S3ObjectLocation` should be a drop-in replacement in all existing code.

## v14.1.0 - 2020-07-14

### Libraries affected

`storage`

### Description

Store Instants in Dynamo as *seconds* since the epoch, not *milliseconds*.

## v14.0.6 - 2020-06-24

No changes, trying to fix build.

## v14.0.5 - 2020-06-24

No changes, trying to fix build.

## v14.0.4 - 2020-06-24

No changes, updates the fmt-sbt-s3-resolver plugin to avoid the dependency error described here: https://github.com/frugalmechanic/fm-sbt-s3-resolver/pull/56

## v14.0.3 - 2020-06-24

No changes - updates build system.

## v14.0.2 - 2020-06-24

No changes - updates build system.

## v14.0.1 - 2020-06-23

No user-facing changes. Splits the build steps in CI for the scala-libs repo to increase potential parallelism in builds, and to make it clearer where failures occur.

## v14.0.0 - 2020-06-23

### Libraries affected

`storage`

### Description

This removes the type `HybridStoreEntry` meaning that for some types `Id` and `Data`, instead of `HybridStore` being a

```
Store[Id, HybridStoreEntry[Data, Metadata]
```

it is now a

```
Store[Id, Data]
```

This is an improvement for any code that might use a hybrid store, as previously anywhere that wanted to use it had to encode the fact that a hybrid store was being used, even though this is just an implementation detail and should not really be part of the interface (we generally should only care that some `Id` maps to some `Data`, and should abstract away the internal details of the storage).

This means that we can remove workarounds such as
[here](https://github.com/wellcomecollection/catalogue/blob/2013710c9bd54fa8644f3b2c3bb618af714a2554/common/big_messaging/src/main/scala/uk/ac/wellcome/bigmessaging/VHS.scala#L50-L77).

Note that the ability to store metadata in the IndexedStore (i.e. DynamoDB) is now not possible: this is only used in the Miro VHS, which does not use the current `HybridStore` interface anyway, being created with an older schema for the data. Storing metadata is such a way should now be considered deprecated.

## v13.1.0 - 2020-06-23

### Libraries affected

`storage_typesafe`

Adds a builder for PrefixTransfer

With config:

```hocon
source.cloudProvider = "aws"
destination {
  cloudProvider = "azure"
  azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
}
```

In code:

```scala
import uk.ac.wellcome.storage.typesafe.PrefixTransferBuilder

val prefixTransfer = PrefixTransferBuilder.build(config)
assert(prefixTransfer.isInstanceOf[S3toAzurePrefixTransfer])
```

## v13.0.0 - 2020-06-23

### Libraries affected

`storage`

### Description

Roll back all the changes in v12.0.0.  We do want to split `ObjectLocation` eventually, but doing it in one fell swoop is far too big a patch (and blocks other changes to these libraries).

## v12.0.0 - 2020-06-22

### Libraries affected

`storage`

### Description

`ObjectLocation` has been removed, and replaced with provider-specific classes `S3ObjectLocation` and `AzureBlobLocation`.
Similar classes exist for `LocationPrefix`.

These two classes have the same semantics as ObjectLocation, but now encode and enforce the associated provider -- so you can't, for example, ask Azure for an object that's stored in S3.

*Note:* Because the field names are now provider-specific, encoding these classes as JSON will give different output.
The library includes decoders so this distinction will be hidden when using the library, but may cause issues for anything else that reads the JSON.

Previously:

```json
{"namespace": "my-great-bucket", "path": "myfile.txt"}
```

Now (S3):

```json
{"bucket": "my-great-bucket", "key": "myfile.txt"}
```

Now (Azure):

```json
{"container": "my-great-container", "name": "myfile.txt"}
```

## v11.5.0 - 2020-06-22

### Libraries affected

`storage_typesafe`

### Description

Adds a `TagsBuilder` object to create the appropriate `Tags` class for the specified `CloudProvider`.

For config:

```hocon
somepath {
  cloudProvider = "azure"
  azure.blobStore.connectionString = "UseDevelopmentStorage=true;"
}
``` 

In code:

```scala
import uk.ac.wellcome.storage.typesafe.TagsBuilder

val tags = TagsBuilder.buildClient(config)

assert(tags.isInstanceOf(AzureBlobMetadata))
```

## v11.4.0 - 2020-06-19

### Libraries affected

`typesafe_app`

### Description

Updates `EnrichConfig` to add:

- `getStringOption`
- `requireString`
- `getIntOption`
- `requireInt`

Deprecates:

- `get[T]`
- `getOrElse[T]`
- `required[T]`

This change is to provide working extraction of optional `Int` & `String` config.

## v11.3.0 - 2020-06-18

### Libraries affected

`storage`

### Description

Adds support for transferring objects between S3 and Azure Blob Storage with `S3toAzureTransfer` and `S3toAzurePrefixTransfer`.

## v11.2.0 - 2020-06-18

### Libraries affected

`storage`

### Description

Adds support for listing the blobs in an Azure Blob Storage container through `AzureBlobItemListing` and `AzureObjectLocationListing`.

## v11.1.0 - 2020-06-18

### Libraries affected

`storage`

### Description

Adds Azure support for `Tags` in the shape of `AzureBlobMetadata`.

```scala
implicit val azureClient: BlobServiceClient =
  new BlobServiceClientBuilder()
    .connectionString("UseDevelopmentStorage=true;")
    .buildClient()

val azureBlobMetadata = new AzureBlobMetadata()

// Get tags
val objectLocation = ObjectLocation("mycontainer","myobject.txt")
val getResult: Either[ReadError, Identified[Ident, T]]  = azureBlobMetadata.get(objectLocation)

val newTags = Map("owner" -> "jill")
val updateResult: Either[UpdateError, Identified[Ident, T]] = azureBlobMetadata.update(objectLocation) { oldTags =>
    oldTags ++ newTags 
}
```

## v11.0.0 - 2020-06-18

### Libraries affected

`storage`

### Description

`RetryableGet` has been replaced in favour of `RetryableReadable`. 

A new trait `Updatable` has been made available and is used in `Tags`.

```scala
trait Updatable[Ident, T] {
  type UpdateEither = Either[UpdateError, Identified[Ident, T]]
  type UpdateFunction = T => Either[UpdateFunctionError, T]

  def update(id: Ident)(updateFunction: UpdateFunction): UpdateEither
}
```

`Tags` now meets the interfaces of  to `RetryableReadable` and `Updatable`.

Consumers will need to update code where `update` and `get` have been used on `S3Tags`.

## v10.1.0 - 2020-06-17

### Libraries affected

`storage`

### Description

Adds support for an [Azure Blob storage](https://azure.microsoft.com/en-gb/services/storage/blobs/) implementation of `StreamStore`.


#### Usage
```scala
import com.azure.storage.blob.{BlobServiceClient, BlobServiceClientBuilder}
import uk.ac.wellcome.storage.store.azure.AzureStreamStore

implicit val azureClient: BlobServiceClient = new BlobServiceClientBuilder()
  .connectionString("UseDevelopmentStorage=true;")
  .buildClient()

val streamStore = new AzureStreamStore()
```

This change also introduces the ability to skip stream length tests as part of the stream store test suite as we are not able to verify those tests for Azure Blob storage with the [Azurite](https://github.com/Azure/Azurite) test containers.

## v10.0.4 - 2020-06-16

### Libraries affected

`messaging`

### Description

Localstack is not compatable with the SQS fixtures here, so remove it.

## v10.0.3 - 2020-06-16

### Libraries affected

`messaging`

### Description

The `getQueueAttribute` was using the default `sqsClient`

## v10.0.2 - 2020-06-15

bump to release all libraries

## v10.0.0 - 2020-06-12

Moved all libs to new repo.