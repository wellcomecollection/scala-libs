package uk.ac.wellcome.storage.azure

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, Prefix}

case class AzureBlobLocation(
  container: String,
  name: String
) extends Location {

  def join(parts: String*): AzureBlobLocation = this.copy(
    name = Paths.get(this.name, parts: _*).normalize().toString
  )
}

case class AzureBlobLocationPrefix(
  container: String,
  namePrefix: String
) extends Prefix[AzureBlobLocation] {

  override def asLocation(parts: String*): AzureBlobLocation =
    AzureBlobLocation(container = container, name = namePrefix).join(parts: _*)
}
