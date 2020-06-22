package uk.ac.wellcome.storage.s3

import uk.ac.wellcome.storage.{Location, Prefix}

case class S3ObjectLocation(
  bucket: String,
  key: String
) extends Location

case class S3ObjectLocationPrefix(
  bucket: String,
  keyPrefix: String
) extends Prefix[S3ObjectLocation]