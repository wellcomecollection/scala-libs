package weco.sierra.models.fields

import java.net.URI
import java.time.LocalDate

// This represents a Sierra Hold object, as described in the Sierra docs:
// https://techdocs.iii.com/sierraapi/Content/zReference/objects/holdObjectDescription.htm

case class SierraHoldStatus(
  code: String,
  name: String
)

case class SierraHold(
  id: URI,
  record: URI,
  pickupLocation: SierraLocation,
  notNeededAfterDate: Option[LocalDate],
  note: Option[String],
  status: SierraHoldStatus
)
