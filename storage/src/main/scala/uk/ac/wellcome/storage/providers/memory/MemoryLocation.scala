package uk.ac.wellcome.storage.providers.memory

import java.nio.file.Paths

case class MemoryLocation(namespace: String, path: String) {
  override def toString: String = s"$namespace/$path"

  def join(parts: String*): MemoryLocation = this.copy(
    path = Paths.get(this.path, parts: _*).normalize().toString
  )

  def asPrefix: MemoryLocationPrefix =
    MemoryLocationPrefix(
      namespace = namespace,
      path = path
    )
}

case class MemoryLocationPrefix(namespace: String, path: String) {
  override def toString = s"$namespace/$path"

  def asLocation(parts: String*): MemoryLocation =
    MemoryLocation(namespace, path).join(parts: _*)
}
