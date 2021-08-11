package weco.sierra.models.fields

import io.circe.Decoder

case class SierraMaterialType(code: String)

object SierraMaterialType {
  implicit val decoder = Decoder.instance[SierraMaterialType](cursor =>
    for {
      id <- cursor.downField("code").as[String]
    } yield {
      SierraMaterialType(id.trim)
  })
}
