package uk.ac.wellcome.storage.maxima

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.{IdentityKey, NoMaximaValueError, Version}
import uk.ac.wellcome.storage.generators.{Record, RecordGenerators}

trait MaximaTestCases
    extends AnyFunSpec
    with Matchers
    with RecordGenerators
    with EitherValues {
  type MaximaStub = Maxima[IdentityKey, Int]

  def withMaxima[R](initialEntries: Map[Version[IdentityKey, Int], Record])(
    testWith: TestWith[MaximaStub, R]): R

  describe("behaves as a Maxima") {

    describe("max") {
      it("finds the maximum value with one matching entry") {
        val id = createIdentityKey

        val initialEntries = Map(
          Version(id, 1) -> createRecord
        )

        withMaxima(initialEntries) {
          _.max(id).value shouldBe 1
        }
      }

      it("finds the maximum value with multiple matching entries") {
        val id = createIdentityKey

        val initialEntries = Map(
          Version(id, 1) -> createRecord,
          Version(id, 2) -> createRecord,
          Version(id, 3) -> createRecord,
          Version(id, 5) -> createRecord
        )

        withMaxima(initialEntries) {
          _.max(id).value shouldBe 5
        }
      }

      it("only looks at the identifier in question") {
        val id = createIdentityKey

        val initialEntries = Map(
          Version(id, 1) -> createRecord,
          Version(id, 2) -> createRecord,
          Version(id, 3) -> createRecord,
          Version(createIdentityKey, 5) -> createRecord
        )

        withMaxima(initialEntries) {
          _.max(id).value shouldBe 3
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
