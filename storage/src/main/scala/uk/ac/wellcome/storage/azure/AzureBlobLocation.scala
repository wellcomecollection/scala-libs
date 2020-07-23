package uk.ac.wellcome.storage.azure

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class AzureBlobLocation(
  container: String,
  name: String
) extends Location {
  override def toString: String =
    s"azure://$container/$name"
}

case class AzureBlobLocationPrefix(
  container: String,
  namePrefix: String
) extends Prefix[AzureBlobLocation] {
  override def toString: String =
    s"azure://$container/$namePrefix"

  def asLocation(parts: String*): AzureBlobLocation =
    AzureBlobLocation(
      container = container,
      name = Paths.get(namePrefix, parts: _*).toString
    )
}
