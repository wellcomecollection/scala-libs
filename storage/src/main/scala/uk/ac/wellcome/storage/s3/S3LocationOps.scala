package uk.ac.wellcome.storage.s3

import java.nio.file.Paths

object S3LocationOps {
  implicit class S3LocationOps(location: S3ObjectLocation) {
    def join(parts: String*): S3ObjectLocation =
      location.copy(
        key = Paths.get(location.key, parts: _*).toString
      )

    def asPrefix: S3ObjectLocationPrefix =
      S3ObjectLocationPrefix(
        bucket = location.bucket,
        keyPrefix = location.key
      )
  }
}
