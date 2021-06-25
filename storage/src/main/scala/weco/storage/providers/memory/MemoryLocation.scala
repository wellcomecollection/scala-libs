package weco.storage.providers.memory

import java.nio.file.Paths

import weco.storage.{Location, Prefix}

case class MemoryLocation(namespace: String, path: String) extends Location {
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

case class MemoryLocationPrefix(namespace: String, path: String)
    extends Prefix[MemoryLocation] {
  override def toString: String = s"$namespace/$path"

  def asLocation(parts: String*): MemoryLocation =
    MemoryLocation(namespace, path).join(parts: _*)

  override def pathPrefix: String = path

  override def parent: Prefix[MemoryLocation] =
    this.copy(path = parentOf(path))
}
