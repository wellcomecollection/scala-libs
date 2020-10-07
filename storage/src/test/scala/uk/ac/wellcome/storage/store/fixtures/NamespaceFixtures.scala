package uk.ac.wellcome.storage.store.fixtures

import uk.ac.wellcome.fixtures.{RandomGenerators, TestWith}
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import uk.ac.wellcome.storage.s3.S3ObjectLocation

trait NamespaceFixtures[Ident, Namespace] {
  def withNamespace[R](testWith: TestWith[Namespace, R]): R

  def createId(implicit namespace: Namespace): Ident
}

trait StringNamespaceFixtures
    extends NamespaceFixtures[String, String]
    with RandomGenerators {
  override def withNamespace[R](testWith: TestWith[String, R]): R =
    testWith(randomAlphanumeric())

  override def createId(implicit namespace: String): String =
    s"$namespace/${randomAlphanumeric()}"
}

trait S3NamespaceFixtures
    extends NamespaceFixtures[S3ObjectLocation, Bucket]
    with S3Fixtures {
  override def withNamespace[R](testWith: TestWith[Bucket, R]): R =
    withLocalS3Bucket { bucket =>
      testWith(bucket)
    }

  override def createId(implicit bucket: Bucket): S3ObjectLocation =
    createS3ObjectLocationWith(bucket)
}
