package weco.sierra.models.fields

import weco.sierra.models.identifiers.SierraItemNumber

case class SierraHoldRequest(
  recordType: String,
  recordNumber: Long,
  pickupLocation: String
)

case object SierraHoldRequest {
  def apply(item: SierraItemNumber): SierraHoldRequest =
    SierraHoldRequest(
      recordType = "i",
      recordNumber = item.withoutCheckDigit.toLong,
      // This field is required non-empty by the Sierra API
      // but seems to have no effect
      pickupLocation = "unspecified"
    )
}
