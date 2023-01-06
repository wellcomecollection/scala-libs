RELEASE_TYPE: patch

Split out creating pre-signed S3 URLs from the S3Uploader class; there's a new S3PresignedUrls class.

This also simplifies the construction of AzurePutBlockFromURLTransfer.

This is more refactoring ahead of the V2 SDK upgrade: you need a different client for pre-signing URLs compared to other operations (`S3Presigner` / `S3Client`), and it makes sense to separate them now.