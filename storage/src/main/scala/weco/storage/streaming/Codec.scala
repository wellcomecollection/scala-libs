package weco.storage.streaming

import java.io.InputStream
import java.nio.charset.{Charset, StandardCharsets}

import io.circe
import io.circe.Json

trait Codec[T] extends Encoder[T] with Decoder[T]

object Codec {
  import EncoderInstances._
  import DecoderInstances._

  implicit val bytesCodec: Codec[Array[Byte]] = new Codec[Array[Byte]] {
    override def fromStream(
      inputStream: InputStream): DecoderResult[Array[Byte]] =
      bytesDecoder.fromStream(inputStream)

    override def toStream(bytes: Array[Byte]): EncoderResult =
      bytesEncoder.toStream(bytes)
  }

  implicit def stringCodec(
    implicit charset: Charset = StandardCharsets.UTF_8
  ): Codec[String] = new Codec[String] {
    override def fromStream(inputStream: InputStream): DecoderResult[String] =
      stringDecoder.fromStream(inputStream)

    override def toStream(t: String): EncoderResult =
      stringEncoder.toStream(t)
  }

  implicit def jsonCodec: Codec[Json] = new Codec[Json] {
    override def fromStream(inputStream: InputStream): DecoderResult[Json] =
      jsonDecoder.fromStream(inputStream)

    override def toStream(t: Json): EncoderResult =
      jsonEncoder.toStream(t)
  }

  implicit def typeCodec[T](
    implicit dec: circe.Decoder[T],
    enc: circe.Encoder[T]
  ): Codec[T] = new Codec[T] {
    override def fromStream(inputStream: InputStream): DecoderResult[T] =
      typeDecoder.fromStream(inputStream)

    override def toStream(t: T): EncoderResult =
      typeEncoder.toStream(t)
  }
}
