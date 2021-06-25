package weco.storage.store.memory

import weco.storage.store.StreamStoreTestCases
import weco.storage.store.fixtures.StringNamespaceFixtures

class MemoryStreamStoreTest
    extends StreamStoreTestCases[
      String,
      String,
      MemoryStreamStore[String],
      MemoryStore[String, Array[Byte]]]
    with MemoryStreamStoreFixtures[String]
    with StringNamespaceFixtures
