package weco.storage.maxima

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import weco.fixtures.TestWith
import weco.storage.{Identified, IdentityKey, NoMaximaValueError, Version}
import weco.storage.generators.{Record, RecordGenerators}

trait MaximaTestCases
    extends AnyFunSpec
    with Matchers
    with RecordGenerators
    with EitherValues {
  type MaximaStub = Maxima[IdentityKey, Version[IdentityKey, Int], Record]

  def withMaxima[R](initialEntries: Map[Version[IdentityKey, Int], Record])(
    testWith: TestWith[MaximaStub, R]): R

  describe("behaves as a Maxima") {

    describe("max") {
      it("finds the maximum value with one matching entry") {
        val id = createIdentityKey
        val r = createRecord

        val initialEntries = Map(
          Version(id, 1) -> r
        )

        withMaxima(initialEntries) {
          _.max(id).value shouldBe Identified(Version(id, 1), r)
        }
      }

      it("finds the maximum value with multiple matching entries") {
        val id = createIdentityKey
        val r = createRecord

        val initialEntries = Map(
          Version(id, 1) -> createRecord,
          Version(id, 2) -> createRecord,
          Version(id, 3) -> createRecord,
          Version(id, 5) -> r
        )

        withMaxima(initialEntries) {
          _.max(id).value shouldBe Identified(Version(id, 5), r)
        }
      }

      it("only looks at the identifier in question") {
        val id = createIdentityKey
        val r = createRecord

        val initialEntries = Map(
          Version(id, 1) -> createRecord,
          Version(id, 2) -> createRecord,
          Version(id, 3) -> r,
          Version(createIdentityKey, 5) -> createRecord
        )

        withMaxima(initialEntries) {
          _.max(id).value shouldBe Identified(Version(id, 3), r)
        }
      }

      it("errors if there are no matching entries") {
        withMaxima(initialEntries = Map.empty) {
          _.max(createIdentityKey).left.value shouldBe a[NoMaximaValueError]
        }
      }
    }
  }
}
