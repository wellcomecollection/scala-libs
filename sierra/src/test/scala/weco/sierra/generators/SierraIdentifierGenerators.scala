package weco.sierra.generators

import weco.fixtures.RandomGenerators
import weco.sierra.models.identifiers.{
  SierraBibNumber,
  SierraHoldingsNumber,
  SierraItemNumber,
  SierraOrderNumber,
  SierraPatronNumber
}

import scala.util.Random

trait SierraIdentifierGenerators extends RandomGenerators {

  // Sierra record numbers should be seven digits long, and start with
  // a non-zero digit
  def createSierraRecordNumberString: String =
    (1000000 + Random.nextInt(9999999 - 1000000)).toString

  def createSierraBibNumber: SierraBibNumber =
    SierraBibNumber(createSierraRecordNumberString)

  def createSierraBibNumbers(count: Int): List[SierraBibNumber] =
    (1 to count).map { _ =>
      createSierraBibNumber
    }.toList

  def createSierraItemNumber: SierraItemNumber =
    SierraItemNumber(createSierraRecordNumberString)

  def createSierraHoldingsNumber: SierraHoldingsNumber =
    SierraHoldingsNumber(createSierraRecordNumberString)

  def createSierraOrderNumber: SierraOrderNumber =
    SierraOrderNumber(createSierraRecordNumberString)

  def createSierraPatronNumber: SierraPatronNumber =
    SierraPatronNumber(createSierraRecordNumberString)
}
