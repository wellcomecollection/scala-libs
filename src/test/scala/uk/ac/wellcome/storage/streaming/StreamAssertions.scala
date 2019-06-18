package uk.ac.wellcome.storage.streaming

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

import org.apache.commons.io.IOUtils
import org.scalatest.{Assertion, Matchers}

trait StreamAssertions extends Matchers {
  def assertStreamsEqual(x: InputStream, y: InputStream): Assertion =
    IOUtils.contentEquals(x, y) shouldBe true

  def assertStreamEquals(inputStream: InputStream with FiniteStream, string: String): Assertion =
    assertStreamEquals(inputStream, string, expectedLength = string.getBytes.length)

  def assertStreamEquals(inputStream: InputStream with FiniteStream, string: String, expectedLength: Long): Assertion =
    assertStreamEquals(
      inputStream,
      bytes = string.getBytes(StandardCharsets.UTF_8),
      expectedLength = expectedLength
    )

  def assertStreamEquals(inputStream: InputStream with FiniteStream, bytes: Array[Byte], expectedLength: Long): Assertion = {
    inputStream.length shouldBe expectedLength

    IOUtils.contentEquals(inputStream, new ByteArrayInputStream(bytes)) shouldBe true
  }
}