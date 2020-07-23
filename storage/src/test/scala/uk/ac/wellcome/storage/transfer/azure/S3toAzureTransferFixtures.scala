package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.fixtures.{AzureFixtures, S3Fixtures}
import uk.ac.wellcome.storage.s3.S3ObjectLocation

trait S3toAzureTransferFixtures extends S3Fixtures with AzureFixtures {
  def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  def withDstNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { container =>
      testWith(container)
    }

  def createSrcLocation(bucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(bucket)

  def createDstLocation(container: Container): AzureBlobLocation =
    createAzureBlobLocationWith(container)
}
