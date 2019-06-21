package uk.ac.wellcome.json

import grizzled.slf4j.Logging
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.json.exceptions.JsonDecodingError

import scala.util.Try

object JsonUtil
    extends AutoDerivation
    with Logging
    with URIConverters {

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults
      .withDiscriminator("type")

  def toJson[T](value: T)(implicit encoder: Encoder[T]): Try[String] = {
    assert(encoder != null)
    Try(value.asJson.noSpaces)
  }

  def toMap[T](json: String)(
    implicit decoder: Decoder[T]): Try[Map[String, T]] = {
    assert(decoder != null)
    fromJson[Map[String, T]](json)
  }

  def fromJson[T](json: String)(implicit decoder: Decoder[T]): Try[T] = {
    assert(decoder != null)
    decode[T](json).toTry.recover {
      case e: Exception =>
        warn(s"Error when trying to decode $json", e)
        throw JsonDecodingError(e)
    }
  }
}
