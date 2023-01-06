package weco.storage.store.s3

import com.amazonaws.services.s3.AmazonS3
import software.amazon.awssdk.services.s3.S3Client
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.StreamStore

class S3StreamStore(val maxRetries: Int = 2)(implicit val s3Client: AmazonS3, val s3ClientV2: S3Client)
    extends StreamStore[S3ObjectLocation]
    with S3StreamReadable
    with S3StreamWritable
