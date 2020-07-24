package uk.ac.wellcome.storage.s3

import java.nio.file.Paths

import io.circe.{Decoder, DecodingFailure, HCursor}
import org.scanamo.DynamoFormat
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.{Location, ObjectLocation, Prefix}

import scala.util.{Failure, Success, Try}

case class S3ObjectLocation(
  bucket: String,
  key: String
) extends Location {
  override def toString: String =
    s"s3://$bucket/$key"

  def join(parts: String*): S3ObjectLocation =
    this.copy(
      key = Paths.get(key, parts: _*).toString
    )

  def asPrefix: S3ObjectLocationPrefix =
    S3ObjectLocationPrefix(bucket, key)

  def toObjectLocation: ObjectLocation =
    ObjectLocation(bucket, key)
}

case class S3ObjectLocationPrefix(
  bucket: String,
  keyPrefix: String
) extends Prefix[S3ObjectLocation] {
  override def toString: String =
    s"s3://$bucket/$keyPrefix"

  def asLocation(parts: String*): S3ObjectLocation =
    S3ObjectLocation(bucket = bucket, key = keyPrefix).join(parts: _*)

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

  case class OldLocation(namespace: String, path: String)

  def createDynamoFormat[T](
    decoder: (String, String) => T,
    keyField: String,
    encoder: T => Map[String, String]
  ): DynamoFormat[T] =
    DynamoFormat.coercedXmap[T, Map[String, String], Throwable](
      read = { values: Map[String, String] =>
        val oldStyle = Try { decoder(values("namespace"), values("path")) }
        val newStyle = Try { decoder(values("bucket"), values(keyField)) }

        (oldStyle, newStyle) match {
          case (Success(location), _) => location
          case (_, Success(location)) => location
          case (_, Failure(err))      => throw err
        }
      }
    )(
      write = { t: T =>
        encoder(t)
      }
    )
}

case object S3ObjectLocation extends S3Decodable {
  def apply(objectLocation: ObjectLocation): S3ObjectLocation =
    S3ObjectLocation(
      bucket = objectLocation.namespace,
      key = objectLocation.path
    )

  implicit val decoder: Decoder[S3ObjectLocation] =
    createDecoder[S3ObjectLocation](keyField = "key") {
      (bucket: String, key: String) =>
        S3ObjectLocation(bucket = bucket, key = key)
    }

  implicit val format: DynamoFormat[S3ObjectLocation] =
    createDynamoFormat[S3ObjectLocation](
      decoder = (bucket: String, key: String) =>
        S3ObjectLocation(bucket = bucket, key = key),
      keyField = "key",
      encoder = (location: S3ObjectLocation) =>
        Map("bucket" -> location.bucket, "key" -> location.key)
    )
}

case object S3ObjectLocationPrefix extends S3Decodable {
  implicit val decoder: Decoder[S3ObjectLocationPrefix] =
    createDecoder[S3ObjectLocationPrefix](keyField = "keyPrefix") {
      (bucket: String, keyPrefix: String) =>
        S3ObjectLocationPrefix(bucket = bucket, keyPrefix = keyPrefix)
    }

  implicit val format: DynamoFormat[S3ObjectLocationPrefix] =
    createDynamoFormat[S3ObjectLocationPrefix](
      decoder = (bucket: String, keyPrefix: String) =>
        S3ObjectLocationPrefix(bucket = bucket, keyPrefix = keyPrefix),
      keyField = "keyPrefix",
      encoder = (location: S3ObjectLocationPrefix) =>
        Map("bucket" -> location.bucket, "keyPrefix" -> location.keyPrefix)
    )
}
