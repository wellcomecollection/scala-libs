package weco.sierra.models.data

import weco.sierra.models.marc.{FixedField, VarField}

case class SierraHoldingsData(
  deleted: Boolean = false,
  suppressed: Boolean = false,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)
