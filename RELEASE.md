RELEASE_TYPE: major

Add some generic classes used in the storage-service:
* ByteRange and ByteRangeUtil
* SizeFinder
* RangedReader
* LargeStreamReader
* ObjectExists
* S3Uploader
* AzurePrefixTransfer and AzureTransfer

Delete classes that are not going to be used:
* AzureBlobMetadata
* S3ToAzurePrefixTransfer and S3ToAzureTransfer