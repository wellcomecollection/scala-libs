package weco.storage.maxima.memory

import weco.fixtures.TestWith
import weco.storage.{IdentityKey, Version}
import weco.storage.generators.Record
import weco.storage.maxima.MaximaTestCases

class MemoryMaximaTest extends MaximaTestCases {
  override def withMaxima[R](
    initialEntries: Map[Version[IdentityKey, Int], Record])(
    testWith: TestWith[MaximaStub, R]): R = {
    val maxima = new MemoryMaxima[IdentityKey, Record] {
      override var entries: Map[Version[IdentityKey, Int], Record] =
        initialEntries
    }

    testWith(maxima)
  }
}
