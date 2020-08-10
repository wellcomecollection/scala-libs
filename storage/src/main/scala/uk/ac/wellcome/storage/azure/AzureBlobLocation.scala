package uk.ac.wellcome.storage.azure

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class AzureBlobLocation(
  container: String,
  name: String
) extends Location {
  override def toString: String =
    s"azure://$container/$name"

  // Having a '.' or '..' in a filesystem path usually indicates "current directory"
  // or "parent directory".  An object store isn't the same as a filesystem,
  // so prevent our code from creating objects with such names.
  require(
    Paths.get(name).normalize().toString == name,
    s"Azure blob name cannot contain '.' or '..' entries, or end in a trailing slash: $name"
  )

  def join(parts: String*): AzureBlobLocation =
    this.copy(
      name = Paths.get(name, parts: _*).toString
    )

  def asPrefix: AzureBlobLocationPrefix =
    AzureBlobLocationPrefix(
      container = container,
      namePrefix = name
    )
}

case class AzureBlobLocationPrefix(
  container: String,
  namePrefix: String
) extends Prefix[AzureBlobLocation] {
  override def toString: String =
    s"azure://$container/$namePrefix"

  // Having a '.' or '..' in a filesystem path usually indicates "current directory"
  // or "parent directory".  An object store isn't the same as a filesystem,
  // so prevent our code from creating objects with such names.
  require(
    Paths.get(namePrefix.stripSuffix("/")).normalize().toString == namePrefix.stripSuffix("/"),
    s"Azure blob name prefix cannot contain '.' or '..' entries: $namePrefix"
  )

  def asLocation(parts: String*): AzureBlobLocation =
    AzureBlobLocation(container = container, name = namePrefix).join(parts: _*)

  override def namespace: String = container
  override def pathPrefix: String = namePrefix

  override def parent: Prefix[AzureBlobLocation] =
    this.copy(namePrefix = parentOf(namePrefix))
}
