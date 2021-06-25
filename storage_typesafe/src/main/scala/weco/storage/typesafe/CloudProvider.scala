package weco.storage.typesafe

object CloudProvider {
  private val awsName = "aws"
  private val azureName = "azure"

  private val nameMap = Map(
    awsName -> AWS,
    azureName -> Azure
  )

  sealed trait CloudProvider {
    val name: String
  }
  case object AWS extends CloudProvider {
    val name: String = awsName
  }
  case object Azure extends CloudProvider {
    val name: String = azureName
  }

  def create(name: String): Option[CloudProvider] =
    nameMap.get(name)
}
