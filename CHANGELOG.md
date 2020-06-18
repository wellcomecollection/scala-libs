# CHANGELOG

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