RELEASE_TYPE: minor

### Libraries affected

`storage`

### Description

Add a constructor for `AzureTypedStore`, `S3toAzureTransfer` and `S3toAzurePrefixTransfer` that take implicit S3 and Azure clients, saving callers from constructing the intermediate classes.
