package uk.ac.wellcome.storage.generators

import java.io.ByteArrayInputStream

import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

import scala.util.Random

trait RandomThings extends Matchers {
  def randomAlphanumeric: String =
    Random.alphanumeric take 8 mkString

  private val lowercaseLatinAlphabet = ('a' to 'z')

  def randomAlphanumericWithLength(length: Int = 8): String =
    Random.alphanumeric take length mkString

  def randomLowercaseLatinAlphabetChar = lowercaseLatinAlphabet(
    Random.nextInt(lowercaseLatinAlphabet.length - 1)
  )

  def randomLowercaseLatinAlphabetString(n: Int = 8) =
    (1 to n) map (_ => randomLowercaseLatinAlphabetChar) mkString

  def randomUTF16String = Random.nextString(8)

  def randomInt(from: Int, to: Int) = {
    val difference = to - from

    assert(difference > 0)

    val randomOffset = Random.nextInt(difference) + 1

    from + randomOffset
  }

  def randomStringOfByteLength(length: Int): String = {
    // Generate bytes within UTF-16 mappable range
    // 0 to 127 maps directly to Unicode code points in the ASCII range
    val chars = (1 to length).map { _ =>
      randomInt(from = 97, to = 122).toByte.toChar
    }

    chars.mkString
  }

  def randomBytes(length: Int = 20): Array[Byte] = {
    val byteArray = Array.fill(length)(0.toByte)

    Random.nextBytes(byteArray)

    byteArray.length > 0 shouldBe true
    byteArray.length shouldBe length

    byteArray
  }

  def randomInputStream(length: Int = 256): InputStreamWithLength = {
    val bytes = randomBytes(length)

    new InputStreamWithLength(new ByteArrayInputStream(bytes), length = length)
  }
}
