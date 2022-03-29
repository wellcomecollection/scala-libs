package weco.sierra.models.fields

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import weco.sierra.models.identifiers.SierraItemNumber

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class SierraHoldRequest(
  recordType: String,
  recordNumber: Long,
  neededBy: Option[String],
  note: Option[String],
  pickupLocation: String
)

case object SierraHoldRequest {
  def apply(item: SierraItemNumber,
            neededBy: Option[LocalDate],
            note: Option[String]): SierraHoldRequest =
    new SierraHoldRequest(
      recordType = "i",
      recordNumber = item.withoutCheckDigit.toLong,
      neededBy = neededBy.map(formatDate),
      note = note,
      // This field is required non-empty by the Sierra API
      // but seems to have no effect
      pickupLocation = "unspecified"
    )

  private lazy val formatDate =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").format(_)

  // While this can/should be done more globally with a Printer, in this case
  // that is a huge headache because HttpClient uses CirceUnmarshalling rather than
  // akkahttpcirce and so doesn't know anything about Printers, and the required
  // refactor to fix that is large and wide-ranging.
  implicit val encoder: Encoder[SierraHoldRequest] =
    deriveEncoder[SierraHoldRequest].mapJson(_.deepDropNullValues)
}
