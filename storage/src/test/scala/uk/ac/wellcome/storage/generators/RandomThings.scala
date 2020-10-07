package uk.ac.wellcome.storage.generators

import java.io.ByteArrayInputStream

import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

import scala.util.Random

trait RandomThings extends RandomGenerators {
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

  def randomStringOfByteLength(length: Int): String = {
    // Generate bytes within UTF-16 mappable range
    // 0 to 127 maps directly to Unicode code points in the ASCII range
    val chars = (1 to length).map { _ =>
      randomInt(from = 97, to = 122).toByte.toChar
    }

    chars.mkString
  }

  def randomInputStream(length: Int = 256): InputStreamWithLength = {
    val bytes = randomBytes(length)

    new InputStreamWithLength(new ByteArrayInputStream(bytes), length = length)
  }
}
