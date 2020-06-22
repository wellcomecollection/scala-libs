package uk.ac.wellcome.storage.s3

import java.nio.file.Paths

import io.circe.{Decoder, DecodingFailure, HCursor}
import uk.ac.wellcome.storage.{Location, Prefix}

case class S3ObjectLocation(
  bucket: String,
  key: String
) extends Location {

  def join(parts: String*): S3ObjectLocation = this.copy(
    key = Paths.get(this.key, parts: _*).normalize().toString
  )
}

case class S3ObjectLocationPrefix(
  bucket: String,
  keyPrefix: String
) extends Prefix[S3ObjectLocation] {

  override def asLocation(parts: String*): S3ObjectLocation =
    S3ObjectLocation(bucket = bucket, key = keyPrefix).join(parts: _*)
}

// Note: the precursor to these classes was ObjectLocation, which used the
// fields "namespace/path" rather than provider-specific terms.
//
// These decoders allow you to read JSON that was encoded with the old code.

trait S3Decodable {
  def createDecoder[T](constructor: (String, String) => T): Decoder[T] =
    (cursor: HCursor) => {
      val oldStyle: Either[DecodingFailure, T] =
        for {
          bucket <- cursor.downField("namespace").as[String]
          key <- cursor.downField("path").as[String]
        } yield constructor(bucket, key)

      val newStyle =
        for {
          bucket <- cursor.downField("bucket").as[String]
          key <- cursor.downField("key").as[String]
        } yield constructor(bucket, key)

      (oldStyle, newStyle) match {
        case (Right(location), _) => Right(location)
        case (_, Right(location)) => Right(location)
        case (_, Left(decodingFailure)) => Left(decodingFailure)
      }
    }
}

case object S3ObjectLocation extends S3Decodable {
  implicit val decoder: Decoder[S3ObjectLocation] =
    createDecoder[S3ObjectLocation] { (bucket: String, key: String) =>
      S3ObjectLocation(bucket = bucket, key = key)
    }
}

case object S3ObjectLocationPrefix extends S3Decodable {
  implicit val decoder: Decoder[S3ObjectLocationPrefix] =
    createDecoder[S3ObjectLocationPrefix] { (bucket: String, key: String) =>
      S3ObjectLocationPrefix(bucket = bucket, keyPrefix = key)
    }
}
