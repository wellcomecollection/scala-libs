### Libraries affected

`storage`

### Description

`ObjectLocation` has been removed, and replaced with provider-specific classes `S3ObjectLocation` and `AzureBlobLocation`.

These two classes have the same semantics as ObjectLocation, but now encode and enforce the associated provider -- so you can't, for example, ask Azure for an object that's stored in S3.
