package weco.storage.generators

import weco.fixtures.RandomGenerators
import weco.storage.providers.memory.{MemoryLocation, MemoryLocationPrefix}

trait MemoryLocationGenerators extends RandomGenerators {
  def createMemoryLocationWith(
    namespace: String = randomAlphanumeric(),
    path: String = randomAlphanumeric()
  ): MemoryLocation =
    MemoryLocation(
      namespace = namespace,
      path = path
    )

  def createMemoryLocation: MemoryLocation =
    createMemoryLocationWith()

  def createMemoryLocationPrefixWith(
    namespace: String = randomAlphanumeric()
  ): MemoryLocationPrefix =
    MemoryLocationPrefix(
      namespace = namespace,
      path = randomAlphanumeric()
    )

  def createMemoryLocationPrefix: MemoryLocationPrefix =
    createMemoryLocationPrefixWith()
}
