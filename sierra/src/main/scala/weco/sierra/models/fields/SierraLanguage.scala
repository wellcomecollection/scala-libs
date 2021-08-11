package weco.sierra.models.fields

// Represents a Language object, as returned by the Sierra API.
// https://techdocs.iii.com/sierraapi/Content/zReference/objects/bibObjectDescription.htm?Highlight=language
case class SierraLanguage(
  code: String,
  name: Option[String]
)

case object SierraLanguage {
  def apply(code: String, name: String): SierraLanguage =
    SierraLanguage(code = code, name = Some(name))
}
