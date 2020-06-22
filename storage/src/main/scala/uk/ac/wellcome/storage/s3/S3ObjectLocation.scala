package uk.ac.wellcome.storage.s3

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

// Note: we use the old namespace/path naming convention because those are the
// names we used for the precursor to this class, ObjectLocation.
//
// Ideally we'd use bucket/key to match the vocabulary used by S3, but that's
// too much for right now. :(
case class S3ObjectLocation(
  namespace: String,
  path: String
) extends Location {

  def join(parts: String*): S3ObjectLocation = this.copy(
    path = Paths.get(this.path, parts: _*).normalize().toString
  )
}

case class S3ObjectLocationPrefix(
  namespace: String,
  path: String
) extends Prefix[S3ObjectLocation] {

  override def asLocation(parts: String*): S3ObjectLocation =
    S3ObjectLocation(namespace = namespace, path = path).join(parts: _*)
}