package uk.ac.wellcome.storage.generators

import java.io.ByteArrayInputStream

import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

trait RandomThings extends RandomGenerators {
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
