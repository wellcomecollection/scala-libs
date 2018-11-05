package uk.ac.wellcome.json

import java.net.{URI, URISyntaxException}

import io.circe.{Decoder, DecodingFailure, Encoder}

private[json] trait URIConverters {
  implicit val uriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[URI](_.toString)

  implicit val uriDecoder: Decoder[URI] = Decoder.instance { cursor =>
    cursor.as[String] match {
      case Right(str) =>
        try Right(new URI(str))
        catch {
          case _: URISyntaxException =>
            Left(DecodingFailure("URI", cursor.history))
        }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[URI]]
    }
  }
}
