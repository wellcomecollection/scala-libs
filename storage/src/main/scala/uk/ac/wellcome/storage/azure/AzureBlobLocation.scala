package uk.ac.wellcome.storage.azure

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class AzureBlobLocation(
  container: String,
  name: String
) extends Location

case class AzureBlobLocationPrefix(
  container: String,
  namePrefix: String
) extends Prefix[AzureBlobLocation] {

  override def asLocation(parts: String*): AzureBlobLocationPrefix =
    AzureBlobLocationPrefix(
      container = container,
      name = Paths.get(namePrefix, parts: _*).normalize().toString
    )
}
