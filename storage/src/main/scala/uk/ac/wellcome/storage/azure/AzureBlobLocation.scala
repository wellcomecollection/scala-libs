package uk.ac.wellcome.storage.azure

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class AzureBlobLocation(
  container: String,
  name: String
) extends Location {
  override def toString: String =
    s"azure://$container/$name"

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

  def asLocation(parts: String*): AzureBlobLocation =
    AzureBlobLocation(container = container, name = namePrefix).join(parts: _*)

  override def pathPrefix: String = namePrefix

  override def parent: Prefix[AzureBlobLocation] =
    this.copy(namePrefix = parentOf(namePrefix))
}
