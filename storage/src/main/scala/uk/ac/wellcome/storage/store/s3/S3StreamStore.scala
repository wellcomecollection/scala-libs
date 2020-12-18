package uk.ac.wellcome.storage.store.s3

import software.amazon.awssdk.services.s3.AmazonS3
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.StreamStore

class S3StreamStore(val maxRetries: Int = 2)(implicit val s3Client: AmazonS3)
    extends StreamStore[S3ObjectLocation]
    with S3StreamReadable
    with S3StreamWritable
