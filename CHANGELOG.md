# CHANGELOG

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