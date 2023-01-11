package weco.storage.store.s3

import org.apache.commons.io.FileUtils
import software.amazon.awssdk.services.s3.S3Client
import weco.storage.s3.S3ObjectLocation
import weco.storage.store.StreamStore

class S3StreamStore(val maxRetries: Int = 2, val partSize: Long = 128 * FileUtils.ONE_MB)(
  implicit val s3Client: S3Client)
    extends StreamStore[S3ObjectLocation]
    with S3StreamReadable
    with S3StreamWritable
