RELEASE_TYPE: minor

This changes the way S3StreamWritable works internally, so it should work better with our applications – in particular it now uses a blocking S3 client, rather than an asynchronous S3TransferManager, which is a better fit for how we do concurrency elsewhere.
