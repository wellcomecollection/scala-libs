package weco.storage.s3

import java.nio.file.Paths

import io.circe.{Decoder, DecodingFailure, HCursor}
import weco.storage.{Location, Prefix}

case class S3ObjectLocation(
  bucket: String,
  key: String
) extends Location {

  // More than one consecutive slash is going to be weird in the console, and
  // is probably indicative of a bug.  Disallow it.
  require(
    !key.contains("//"),
    s"S3 object key cannot include multiple consecutive slashes: $key"
  )

  require(
    !key.endsWith("/"),
    s"S3 object key cannot end with a slash: $key"
  )

  // Having a '.' or '..' in a filesystem path usually indicates "current directory"
  // or "parent directory".  Having either of these in an S3 key causes issues in
  // the S3 console, so prevent our code from creating objects with such keys.
  require(
    Paths.get(key).normalize().toString == key,
    s"S3 object key cannot contain '.' or '..' entries, or end in a trailing slash: $key"
  )

  override def toString: String =
    s"s3://$bucket/$key"

  def join(parts: String*): S3ObjectLocation =
    this.copy(
      key = Paths.get(key, parts: _*).toString
    )

  def asPrefix: S3ObjectLocationPrefix =
    S3ObjectLocationPrefix(bucket, key)
}

case class S3ObjectLocationPrefix(
  bucket: String,
  keyPrefix: String
) extends Prefix[S3ObjectLocation] {

  // More than one consecutive slash is going to be weird in the console, and
  // is probably indicative of a bug.  Disallow it.
  require(
    !keyPrefix.contains("//"),
    s"S3 key prefix cannot include multiple consecutive slashes: $keyPrefix"
  )

  // Having a '.' or '..' in the path usually indicates "current directory" or
  // "parent directory".  Having either of these in an S3 key causes issues in
  // the S3 console, so prevent our code from creating objects with such keys.
  require(
    Paths.get(keyPrefix.stripSuffix("/")).normalize().toString == keyPrefix
      .stripSuffix("/"),
    s"S3 key prefix cannot contain '.' or '..' entries: $keyPrefix"
  )

  override def toString: String =
    s"s3://$bucket/$keyPrefix"

  def asLocation(parts: String*): S3ObjectLocation =
    S3ObjectLocation(bucket = bucket, key = keyPrefix).join(parts: _*)

  override def namespace: String = bucket
  override def pathPrefix: String = keyPrefix

  override def parent: Prefix[S3ObjectLocation] =
    this.copy(keyPrefix = parentOf(keyPrefix))
}

// Note: the precursor to these classes was ObjectLocation, which used the
// fields "namespace/path" rather than provider-specific terms.
//
// These decoders allow you to read JSON that was encoded with the old code.
trait S3Decodable {
  def createDecoder[T](keyField: String)(
    constructor: (String, String) => T): Decoder[T] =
    (cursor: HCursor) => {
      val oldStyle: Either[DecodingFailure, T] =
        for {
          bucket <- cursor.downField("namespace").as[String]
          key <- cursor.downField("path").as[String]
        } yield constructor(bucket, key)

      val newStyle =
        for {
          bucket <- cursor.downField("bucket").as[String]
          key <- cursor.downField(keyField).as[String]
        } yield constructor(bucket, key)

      (oldStyle, newStyle) match {
        case (Right(location), _)       => Right(location)
        case (_, Right(location))       => Right(location)
        case (_, Left(decodingFailure)) => Left(decodingFailure)
      }
    }
}

case object S3ObjectLocation extends S3Decodable {
  implicit val decoder: Decoder[S3ObjectLocation] =
    createDecoder[S3ObjectLocation](keyField = "key") {
      (bucket: String, key: String) =>
        S3ObjectLocation(bucket = bucket, key = key)
    }
}

case object S3ObjectLocationPrefix extends S3Decodable {
  implicit val decoder: Decoder[S3ObjectLocationPrefix] =
    createDecoder[S3ObjectLocationPrefix](keyField = "keyPrefix") {
      (bucket: String, keyPrefix: String) =>
        S3ObjectLocationPrefix(bucket = bucket, keyPrefix = keyPrefix)
    }
}
