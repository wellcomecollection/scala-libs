RELEASE_TYPE: minor

Bump the AWS SDK from 2.28.15 to 2.55.0, which moves the transitive netty from 4.1.112.Final to 4.1.138.Final.

From 2.30 the SDK adds a CRC32 checksum to every S3 part upload by default. `S3MultipartUploader` now declares CRC32 when it creates a multipart upload and passes each part's checksum back when it completes the upload.

Pin jackson-core, jackson-databind and jackson-module-scala to 2.18.10 in the elasticsearch library. elastic4s 8.11.5 resolves jackson 2.14.3, and there is no later elastic4s release under the `com.sksamuel.elastic4s` group to move to.

These changes clear open security advisories on netty and jackson here and in the downstream catalogue-pipeline and catalogue-api repositories.
