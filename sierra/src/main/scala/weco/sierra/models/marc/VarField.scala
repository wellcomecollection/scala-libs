package weco.sierra.models.marc

import io.circe.generic.extras.JsonKey

// Examples of varFields from the Sierra JSON:
//
//    {
//      "fieldTag": "b",
//      "content": "X111658"
//    }
//
//    {
//      "fieldTag": "a",
//      "marcTag": "949",
//      "ind1": "0",
//      "ind2": "0",
//      "subfields": [
//        {
//          "tag": "1",
//          "content": "STAX"
//        },
//        {
//          "tag": "2",
//          "content": "sepam"
//        }
//      ]
//    }
//
case class VarField(
  content: Option[String] = None,
  marcTag: Option[String] = None,
  fieldTag: Option[String] = None,
  @JsonKey("ind1") indicator1: Option[String] = None,
  @JsonKey("ind2") indicator2: Option[String] = None,
  subfields: List[Subfield] = Nil
)

case object VarField {
  def apply(fieldTag: String, content: String): VarField =
    VarField(
      fieldTag = Some(fieldTag),
      content = Some(content)
    )

  def apply(marcTag: String, subfields: List[Subfield]): VarField =
    VarField(marcTag = Some(marcTag), subfields = subfields)
}
