package uk.ac.wellcome.storage.s3

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.storage.ObjectLocation

class S3ObjectLocationTest extends AnyFunSpec with Matchers {
  it("creates a location from a prefix") {
    val prefix = S3ObjectLocationPrefix(
      namespace = "my-test-bucket",
      path = "png-objects"
    )

    val location = prefix.asLocation("cats", "cat1.png")

    location shouldBe S3ObjectLocation(
      namespace = "my-test-bucket",
      path = "png-objects/cats/cat1.png"
    )
  }

  it("decodes from an old-style ObjectLocation") {
    import uk.ac.wellcome.json.JsonUtil._

    val location = ObjectLocation(
      namespace = "my-bucket",
      path = "myfile.txt"
    )

    println(toJson(location).get)

    true shouldBe false
  }
}
