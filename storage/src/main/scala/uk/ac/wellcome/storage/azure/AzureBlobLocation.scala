package uk.ac.wellcome.storage.azure

import java.nio.file.Paths

import uk.ac.wellcome.storage.{Location, ObjectLocation, Prefix}

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

  // TODO: Bridging code while we split the locations; remove eventually
  def toObjectLocation: ObjectLocation =
    ObjectLocation(container, name)

}

case class AzureBlobLocationPrefix(
  container: String,
  namePrefix: String
) extends Prefix[AzureBlobLocation] {
  override def toString: String =
    s"azure://$container/$namePrefix"

  def asLocation(parts: String*): AzureBlobLocation =
    AzureBlobLocation(container, namePrefix).join(parts: _*)
}
