package weco.sierra.models.fields

import weco.sierra.models.data.SierraItemData

case class SierraItemDataEntries(
  total: Int,
  start: Int,
  entries: Seq[SierraItemData]
)
