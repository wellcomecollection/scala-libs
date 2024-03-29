package weco.sierra.models.errors

import weco.sierra.models.data.SierraItemData
import weco.sierra.models.identifiers.SierraItemNumber

sealed trait SierraItemLookupError

object SierraItemLookupError {
  case object ItemNotFound extends SierraItemLookupError

  case class MissingItems(
    missingItems: Seq[SierraItemNumber],
    itemsReturned: Seq[SierraItemData]
  ) extends SierraItemLookupError

  case class UnknownError(errorCode: SierraErrorCode)
      extends SierraItemLookupError
}
