package weco.sierra.models.marc

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.json.JsonUtil._

class MarcFieldTest extends AnyFunSpec with Matchers {

  it("reads a JSON string as a long-form VarField") {
    val jsonString = s"""{
      "fieldTag": "n",
      "marcTag": "008",
      "ind1": " ",
      "ind2": " ",
      "subfields": [
        {
          "tag": "a",
          "content": "An armada of armadillos"
        },
        {
          "tag": "b",
          "content": "A bonanza of bears"
        },
        {
          "tag": "c",
          "content": "A cacophany of crocodiles"
        }
      ]
    }"""

    val expectedVarField = VarField(
      fieldTag = Some("n"),
      marcTag = Some("008"),
      indicator1 = Some(" "),
      indicator2 = Some(" "),
      subfields = List(
        Subfield(tag = "a", content = "An armada of armadillos"),
        Subfield(tag = "b", content = "A bonanza of bears"),
        Subfield(tag = "c", content = "A cacophany of crocodiles")
      )
    )

    val varField = fromJson[VarField](jsonString).get
    varField shouldBe expectedVarField
  }

  it("reads a JSON string as a short-form VarField") {
    val jsonString = s"""{
      "fieldTag": "c",
      "content": "Enjoying an event with enormous eagles"
    }"""

    val expectedVarField = VarField(
      fieldTag = "c",
      content = "Enjoying an event with enormous eagles"
    )

    fromJson[VarField](jsonString).get shouldBe expectedVarField
  }
}
