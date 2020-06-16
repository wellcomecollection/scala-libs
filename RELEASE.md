RELEASE_TYPE: minor

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