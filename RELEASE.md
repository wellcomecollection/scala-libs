RELEASE_TYPE: patch

Remember to close streams that get opened in S3toAzureTransfer, otherwise you get the error:

> Unable to execute HTTP request: Timeout waiting for connection from pool
