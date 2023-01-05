RELEASE_TYPE: minor

Remove batchSize from S3ObjectLocationListing.

This is never set to a non-default value, so there should be no impact on downstream code.

Also contains some refactoring ahead of upgrading to the AWS Java V2 SDK for S3.
