package weco.storage.store.memory

import weco.storage.generators.{Record, RecordGenerators}
import weco.storage.store.StoreWithOverwritesTestCases
import weco.storage.store.fixtures.StringNamespaceFixtures

class MemoryStoreTest
    extends StoreWithOverwritesTestCases[
      String,
      Record,
      String,
      MemoryStore[String, Record]]
    with MemoryStoreFixtures[String, Record, String]
    with StringNamespaceFixtures
    with RecordGenerators {

  override def createT: Record =
    createRecord
}
