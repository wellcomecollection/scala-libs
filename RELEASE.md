RELEASE_TYPE: patch

### Libraries affected

`storage`

### Description

The `S3Tags` class will now retry writing tags to S3 if it receives a transient error from the SetObjectTagging API.
