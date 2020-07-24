RELEASE_TYPE: major

Change the `S3Listing` and `S3Transfer` classes to use the new-style locations.

Replace all uses of `ObjectLocation` with `S3ObjectLocation` to get the new behaviour; the two classes are compatible.
