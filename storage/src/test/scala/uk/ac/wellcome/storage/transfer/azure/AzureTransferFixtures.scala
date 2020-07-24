package uk.ac.wellcome.storage.transfer.azure

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.azure.AzureBlobLocation
import uk.ac.wellcome.storage.fixtures.AzureFixtures.Container
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.fixtures.{AzureFixtures, S3Fixtures}
import uk.ac.wellcome.storage.s3.S3ObjectLocation

trait AzureTransferFixtures extends AzureFixtures with S3Fixtures {
  def withSrcNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { srcBucket =>
      testWith(srcBucket)
    }

  def withDstNamespace[R](testWith: TestWith[Container, R]): R =
    withAzureContainer { dstContainer =>
      testWith(dstContainer)
    }

  def createSrcLocation(srcBucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(srcBucket)

  def createDstLocation(dstContainer: Container): AzureBlobLocation =
    createAzureBlobLocationWith(dstContainer)

  def srcToObjectLocation(srcLocation: S3ObjectLocation): ObjectLocation = srcLocation.toObjectLocation

  def dstToObjectLocation(dstLocation: AzureBlobLocation): AzureBlobLocation = dstLocation
}
