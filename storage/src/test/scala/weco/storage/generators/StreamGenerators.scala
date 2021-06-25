package weco.storage.generators

import java.io.ByteArrayInputStream

import weco.fixtures.RandomGenerators
import weco.storage.streaming.InputStreamWithLength

trait StreamGenerators extends RandomGenerators {
  def createInputStream(length: Int = 256): InputStreamWithLength = {
    val bytes = randomBytes(length)

    new InputStreamWithLength(new ByteArrayInputStream(bytes), length = length)
  }
}
