RELEASE_TYPE: major

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
