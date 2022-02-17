package weco.sierra.models.fields

import weco.sierra.models.identifiers.SierraItemNumber

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class SierraHoldRequest(
  recordType: String,
  recordNumber: Long,
  neededBy: String,
  pickupLocation: String
)

case object SierraHoldRequest {
  def apply(item: SierraItemNumber, neededBy: LocalDate): SierraHoldRequest =
    SierraHoldRequest(
      recordType = "i",
      recordNumber = item.withoutCheckDigit.toLong,
      neededBy = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(neededBy),
      // This field is required non-empty by the Sierra API
      // but seems to have no effect
      pickupLocation = "unspecified"
    )
}
