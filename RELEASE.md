RELEASE_TYPE: patch

Remove a now-unused custom DynamoFormat for S3ObjectLocation.
We wrote this format when we changed the S3ObjectLocation model from namespace/path to bucket/key, to allow our Scala apps to read old-style locations.
We've now migrated our DynamoDB tables to use the new-style locations exclusively, so this translation code can be removed.