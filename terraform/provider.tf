provider "aws" {
  region = "eu-west-1"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-admin"
  }

  default_tags {
    tags = {
      TerraformConfigurationURL = "https://github.com/wellcomecollection/scala-libs/tree/main/terraform"
      Environment               = "Production"
      Department                = "Digital Platform"
      Division                  = "Wellcome Collection"
      Use                       = "scala-libs release bucket"
    }
  }
}
