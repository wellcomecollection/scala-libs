RELEASE_TYPE: minor

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
