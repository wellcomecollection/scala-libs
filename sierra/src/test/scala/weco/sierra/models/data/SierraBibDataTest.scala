package weco.sierra.models.data

import org.scalatest.TryValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.JsonUtil._
import weco.sierra.models.fields.SierraLanguage

class SierraBibDataTest extends AnyFunSpec with Matchers with TryValues {
  it("decodes a bibData with language") {
    val bibDataJson =
      s"""
         |{
         |  "lang": {
         |    "code": "eng",
         |    "name": "English"
         |  }
         |}""".stripMargin

    fromJson[SierraBibData](bibDataJson).get shouldBe SierraBibData(
      lang = Some(SierraLanguage("eng", "English")))
  }

  it("decodes a bib with no language name") {
    // This is a minimal example based on b16675617, as retrieved 19 April 2021
    // It was failing in the Sierra transformer.
    val bibDataJson =
      s"""{
         |  "deleted": false,
         |  "suppressed": false,
         |  "lang": {
         |    "code": "tgl"
         |  },
         |  "locations": [],
         |  "fixedFields": {
         |    "24": {
         |      "label": "LANG",
         |      "value": "tgl"
         |    }
         |  },
         |  "varFields": []
         |}""".stripMargin

    val bibData = fromJson[SierraBibData](bibDataJson).success.value
    bibData.lang shouldBe Some(SierraLanguage(code = "tgl", name = None))
  }
}
