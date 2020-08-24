RELEASE_TYPE: patch

### Libraries affected

`storage`

### Description

Make the warning introduced in v19.5.3 a bit less chatty.  In particular, the Listing classes no longer warn if you try to list a prefix with an empty path -- i.e., the complete contents of an S3 bucket or Azure container.
