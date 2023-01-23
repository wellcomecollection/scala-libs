RELEASE_TYPE: minor

Remove a bunch of one-line builder methods (e.g. S3Builder.buildS3Client), which can be inlined whenever called, removing an unnecessary bit of abstraction.