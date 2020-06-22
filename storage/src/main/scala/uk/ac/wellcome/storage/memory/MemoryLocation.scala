package uk.ac.wellcome.storage.memory

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class MemoryLocation(path: String) extends Location

case class MemoryLocationPrefix(prefix: String) extends Prefix[MemoryLocation] {
  override def asLocation(parts: String*): MemoryLocation =
    MemoryLocation(
      path = Paths.get(prefix, parts: _*).normalize().toString
    )
}