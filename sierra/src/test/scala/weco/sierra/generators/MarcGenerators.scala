package weco.sierra.generators

import weco.sierra.models.marc.{Subfield, VarField}

trait MarcGenerators {
  def createVarFieldWith(
    marcTag: String,
    indicator1: Option[String] = None,
    indicator2: Option[String] = None,
    subfields: List[Subfield] = List(),
    content: Option[String] = None
  ): VarField =
    VarField(
      marcTag = Some(marcTag),
      content = content,
      indicator1 = indicator1,
      indicator2 = indicator2,
      subfields = subfields
    )

  def createVarFieldWith(
    marcTag: String,
    indicator2: String,
    subfields: List[Subfield]
  ): VarField =
    createVarFieldWith(
      marcTag = marcTag,
      indicator2 = Some(indicator2),
      subfields = subfields
    )
}
