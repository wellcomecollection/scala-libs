package uk.ac.wellcome.storage.generators

import java.io.ByteArrayInputStream

import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.storage.streaming.InputStreamWithLength

trait RandomThings extends RandomGenerators {
  def randomInputStream(length: Int = 256): InputStreamWithLength = {
    val bytes = randomBytes(length)

    new InputStreamWithLength(new ByteArrayInputStream(bytes), length = length)
  }
}
