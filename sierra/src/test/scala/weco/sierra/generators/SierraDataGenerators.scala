package weco.sierra.generators

import weco.sierra.models.data.{SierraBibData, SierraItemData, SierraOrderData}
import weco.sierra.models.fields.{SierraLanguage, SierraLocation, SierraMaterialType}
import weco.sierra.models.identifiers.SierraItemNumber
import weco.sierra.models.marc.{FixedField, VarField}

trait SierraDataGenerators extends SierraIdentifierGenerators {
  def createSierraBibDataWith(
    lang: Option[SierraLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    locations: Option[List[SierraLocation]] = None,
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      lang = lang,
      materialType = materialType,
      locations = locations,
      varFields = varFields
    )

  def createSierraBibData: SierraBibData = createSierraBibDataWith()

  def createSierraItemDataWith(
    id: SierraItemNumber = createSierraItemNumber,
    location: Option[SierraLocation] = None,
    copyNo: Option[Int] = None,
    holdCount: Option[Int] = Some(0),
    fixedFields: Map[String, FixedField] = Map(),
    varFields: List[VarField] = Nil
  ): SierraItemData =
    SierraItemData(
      id = id,
      location = location,
      copyNo = copyNo,
      holdCount = holdCount,
      fixedFields = fixedFields,
      varFields = varFields
    )

  def createSierraItemData: SierraItemData = createSierraItemDataWith()

  def createSierraOrderDataWith(
    fixedFields: Map[String, FixedField] = Map()
  ): SierraOrderData =
    SierraOrderData(
      fixedFields = fixedFields
    )

  def createSierraOrderData: SierraOrderData = createSierraOrderDataWith()
}
