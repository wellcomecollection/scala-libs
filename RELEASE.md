RELEASE_TYPE: minor

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