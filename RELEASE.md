RELEASE_TYPE: major

Change all the Azure-related storage classes to use the new-style locations.

Replace all uses of `ObjectLocation` with `AzureBlobLocation` to get the new behaviour; the two classes are compatible.
