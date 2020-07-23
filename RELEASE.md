RELEASE_TYPE: minor

This release introduces four new classes in the **storage** library:

-   `S3ObjectLocation`
-   `S3ObjectLocationPrefix`
-   `AzureBlobLocation`
-   `AzureBlobLocationPrefix`

which extend new traits `Location` and `Prefix`, respectively.

These will eventually replace the existing `ObjectLocation` class, but `S3ObjectLocation` should be a drop-in replacement in all existing code.
