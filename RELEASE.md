RELEASE_TYPE: minor

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
