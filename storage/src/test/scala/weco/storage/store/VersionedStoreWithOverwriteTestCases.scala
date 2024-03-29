package weco.storage.store

import grizzled.slf4j.Logging
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import weco.fixtures.TestWith
import weco.storage._
import weco.storage.store.fixtures.VersionedStoreFixtures

trait VersionedStoreWithOverwriteTestCases[Id, T, VersionedStoreContext]
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with Logging
    with VersionedStoreFixtures[Id, Int, T, VersionedStoreContext]
    with StoreWithoutOverwritesTestCases[
      Version[Id, Int],
      T,
      String,
      VersionedStoreContext] {

  def createIdent: Id
  def createT: T

  def withVersionedStoreImpl[R](initialEntries: Entries = Map.empty)(
    testWith: TestWith[VersionedStoreImpl, R]): R

  def withFailingGetVersionedStore[R](initialEntries: Entries = Map.empty)(
    testWith: TestWith[VersionedStoreImpl, R]): R
  def withFailingPutVersionedStore[R](initialEntries: Entries = Map.empty)(
    testWith: TestWith[VersionedStoreImpl, R]): R

  describe("it behaves as a VersionedStore") {
    describe("init") {
      it("stores a new record at the starting version") {
        withVersionedStoreImpl() { versionedStore =>
          val id = createIdent
          val t = createT

          val result = versionedStore.init(id)(t)
          val value = result.value

          value.identifiedT shouldBe t
          value.id shouldBe Version(id, 0)
        }
      }

      it("does not store a new record if one exists") {
        val id = createIdent

        val t1 = createT
        val t2 = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t1)
        ) { store =>
          val result = store.init(id)(t2)
          val err = result.left.value

          err shouldBe a[VersionAlreadyExistsError]
        }
      }

      it("fails if the underlying store fails to put") {
        withFailingPutVersionedStore() { store =>
          val id = createIdent
          val t = createT

          val result = store.init(id)(t)
          result.left.value shouldBe a[StoreWriteError]
        }
      }
    }

    describe("putLatest") {
      it("stores a new record") {
        withVersionedStoreImpl() { store =>
          val id = createIdent

          val t = createT

          val result = store.putLatest(id)(t)

          debug(s"Got: $result")

          val value = result.value

          value.identifiedT shouldEqual t
          value.id shouldEqual Version(id, 0)
        }
      }

      it("increments version monotonically if no version is specified") {
        val id = createIdent

        val t1 = createT
        val t2 = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t1)
        ) { store =>
          val result = store.putLatest(id)(t2)
          val err = result.value

          err.identifiedT shouldEqual t2
          err.id shouldEqual Version(id, 1)
        }
      }

      it("increments version monotonically (despite gaps in the version sequence)") {
        val id = createIdent

        val t2 = createT
        val t3 = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 2) -> t2)
        ) { store =>
          val result = store.putLatest(id)(t3)
          val value = result.value

          value.identifiedT shouldEqual t3
          value.id shouldEqual Version(id, 3)
        }
      }

      it("fails if the underlying store fails to put") {
        withFailingPutVersionedStore() { store =>
          val id = createIdent
          val t = createT

          val result = store.putLatest(id)(t)
          result.left.value shouldBe a[StoreWriteError]
        }
      }
    }

    describe("put") {
      it(
        "puts to a version if specified and that version represents an increase") {
        val id = createIdent

        val t1 = createT
        val t2 = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t1)
        ) { store =>
          val result = store.put(Version(id, 1))(t2)
          val err = result.value

          err.identifiedT shouldEqual t2
          err.id shouldEqual Version(id, 1)
        }
      }

      it("allows gaps in the version sequence") {
        val id = createIdent

        val t0 = createT
        val t2 = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t0)
        ) { store =>
          val result = store.put(Version(id, 2))(t2)
          val value = result.value

          value.identifiedT shouldEqual t2
          value.id shouldEqual Version(id, 2)
        }
      }

      it("refuses to add a version lower than the latest version") {
        val id = createIdent

        val t2 = createT
        val t3 = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 3) -> t3)
        ) { store =>
          val result = store.put(Version(id, 1))(t2)
          val err = result.left.value

          err shouldBe a[HigherVersionExistsError]
        }
      }

      it("allows writing the same value twice to a given id/version") {
        val id = createIdent
        val t = createT

        withVersionedStoreImpl() { store =>
          store.put(Version(id, 0))(t) shouldBe Right(
            Identified(Version(id, 0), t))
          store.put(Version(id, 0))(t) shouldBe Right(
            Identified(Version(id, 0), t))
          store.put(Version(id, 0))(t) shouldBe Right(
            Identified(Version(id, 0), t))
        }
      }

      it("refuses to overwrite an existing id/version with a different value") {
        val id = createIdent

        withVersionedStoreImpl() { store =>
          store.put(Version(id, 0))(createT) shouldBe a[Right[_, _]]
          store
            .put(Version(id, 0))(createT)
            .left
            .value shouldBe a[VersionAlreadyExistsError]
        }
      }
    }

    describe("get") {
      it("gets a stored record") {
        val id = createIdent
        val t = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          val result = store.getLatest(id)
          val value = result.value

          value shouldBe Identified(Version(id, 0), t)
        }
      }

      it("fails when getting a non-existent version on an id") {
        val id = createIdent
        val t = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          store.get(Version(id, 0)).value shouldBe Identified(Version(id, 0), t)
          store.get(Version(id, 1)).left.value shouldBe a[NoVersionExistsError]
        }
      }

      it("fails when getting a non-existent id and version pair") {
        withVersionedStoreImpl() { store =>
          val id = createIdent

          store.get(Version(id, 1)).left.value shouldBe a[NoVersionExistsError]
        }
      }

      it("fails when getting a non-existent id") {
        withVersionedStoreImpl() { store =>
          val id = createIdent

          store.getLatest(id).left.value shouldBe a[NoVersionExistsError]
        }
      }

      it("fails if the underlying store fails to get") {
        withFailingGetVersionedStore() { store =>
          val id = createIdent

          store.get(Version(id, 1)).left.value shouldBe a[StoreReadError]
        }
      }
    }

    describe("upsert") {
      it("updates an existing id") {
        val id = createIdent

        val t = createT
        val updatedT = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          val f = (_: T) => Right(updatedT)

          val upsertResult = store.upsert(id)(t)(f)

          upsertResult.value shouldBe Identified(Version(id, 1), updatedT)
          store.get(Version(id, 1)).value shouldBe Identified(
            Version(id, 1),
            updatedT)
        }
      }

      it("writes when an id does not exist") {
        withVersionedStoreImpl() { store =>
          val id = createIdent
          val t = createT
          val otherT = createT

          val f = (_: T) => Right(otherT)

          val upsertResult = store.upsert(id)(t)(f)

          upsertResult.value shouldBe Identified(Version(id, 0), t)
          store.get(Version(id, 0)).value shouldBe Identified(Version(id, 0), t)
        }
      }
    }

    describe("update") {
      it("updates an existing id") {
        val id = createIdent

        val t = createT
        val updatedT = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          val f = (_: T) => Right(updatedT)

          val update = store
            .update(id)(f)

          update.value shouldBe Identified(Version(id, 1), updatedT)
          store.get(Version(id, 1)).value shouldBe Identified(
            Version(id, 1),
            updatedT)
        }
      }

      it("refuses to write when an id does not exist") {
        withVersionedStoreImpl() { store =>
          val id = createIdent
          val t = createT

          val f = (_: T) => Right(t)

          val update = store
            .update(id)(f)

          update.left.value shouldBe a[UpdateNoSourceError]
        }
      }

      it("fails when an update function returns a Left[UpdateNotApplied]") {
        val id = createIdent
        val t = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          val err = new Throwable("BOOM!")

          val f = (_: T) => Left(UpdateNotApplied(err))

          val update = store
            .update(id)(f)

          val result = update.left.value
          result shouldBe a[UpdateNotApplied]
          result.e shouldBe err
        }
      }

      it("fails when an update function throws") {
        val id = createIdent
        val t = createT

        withVersionedStoreImpl(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          val err = new Throwable("BOOM!")
          val f = (_: T) => throw err

          val update = store
            .update(id)(f)

          val result = update.left.value
          result shouldBe a[UpdateUnexpectedError]
          result.e shouldBe err
        }
      }

      it("fails if the underlying store fails to write") {
        val id = createIdent
        val t = createT

        val version = Version(id, 0)

        withFailingPutVersionedStore(initialEntries = Map(version -> t)) {
          store =>
            val f = (_: T) => Right(t)

            val update = store
              .update(id)(f)

            update.left.value shouldBe a[UpdateWriteError]
        }
      }

      it("fails if the underlying store fails to read") {
        val id = createIdent

        val t = createT

        withFailingGetVersionedStore(
          initialEntries = Map(Version(id, 0) -> t)
        ) { store =>
          val f = (_: T) => Right(t)

          val update = store
            .update(id)(f)

          update.left.value shouldBe a[UpdateReadError]
        }
      }
    }

    it("is internally consistent") {
      val id = createIdent

      val t0 = createT
      val t1 = createT
      val t2 = createT
      val t3 = createT
      val t4 = createT

      withVersionedStoreImpl() { store =>
        store.init(id)(t0) shouldBe a[Right[_, _]]
        store.getLatest(id).value shouldBe Identified(Version(id, 0), t0)
        store.get(Version(id, 0)).value shouldBe Identified(Version(id, 0), t0)

        store.putLatest(id)(t1) shouldBe a[Right[_, _]]
        store.getLatest(id).value shouldBe Identified(Version(id, 1), t1)

        store.put(Version(id, 2))(t2) shouldBe a[Right[_, _]]
        store.getLatest(id).value shouldBe Identified(Version(id, 2), t2)

        val f1 = (_: T) => Right(t3)
        val f2 = (_: T) => Right(t4)

        store.upsert(id)(t0)(f1) shouldBe a[Right[_, _]]
        store.getLatest(id).value shouldBe Identified(Version(id, 3), t3)

        store.update(id)(f2) shouldBe a[Right[_, _]]
        store.getLatest(id).value shouldBe Identified(Version(id, 4), t4)
      }
    }
  }
}
