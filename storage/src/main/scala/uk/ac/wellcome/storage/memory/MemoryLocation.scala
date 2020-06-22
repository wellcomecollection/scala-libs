package uk.ac.wellcome.storage.memory

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class MemoryLocation(namespace: String, path: String) extends Location {
  override def join(parts: String*): MemoryLocation =
    this.copy(
      path = Paths.get(this.path, parts: _*).normalize().toString
    )
}

case class MemoryLocationPrefix(namespace: String, path: String) extends Prefix[MemoryLocation] {
  override def asLocation(parts: String*): MemoryLocation =
    MemoryLocation(namespace = namespace, path = path).join(parts: _*)
}
