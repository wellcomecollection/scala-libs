package weco.storage.streaming

import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}
import io.circe
import io.circe.Json
import weco.Logging
import weco.json.JsonUtil.toJson
import weco.storage.{EncoderError, JsonEncodingError}

import scala.util.{Failure, Success}

trait Encoder[T] {
  type EncoderResult = Either[EncoderError, InputStreamWithLength]

  def toStream(t: T): EncoderResult
}

object EncoderInstances extends Logging {
  implicit val bytesEncoder: Encoder[Array[Byte]] =
    (bytes: Array[Byte]) =>
      Right(
        new InputStreamWithLength(
          new ByteArrayInputStream(bytes),
          length = bytes.length
        )
    )

  implicit def stringEncoder(
    implicit charset: Charset = StandardCharsets.UTF_8
  ): Encoder[String] =
    (s: String) => {
      trace(s"Encoding string <$s> with charset <$charset>")

      bytesEncoder.toStream(s.getBytes(charset))
    }

  // Circe uses the UTF-8 encoder internally
  implicit val jsonEncoder: Encoder[Json] =
    (t: Json) => stringEncoder.toStream(t.noSpaces)

  implicit def typeEncoder[T](implicit enc: circe.Encoder[T]): Encoder[T] =
    (t: T) =>
      toJson(t) match {
        case Success(jsonString) => stringEncoder.toStream(jsonString)
        case Failure(err)        => Left(JsonEncodingError(err))
    }
}
