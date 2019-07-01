package uk.ac.wellcome.fixtures

import java.time.Instant
import java.util.UUID

import scala.util.Random

trait RandomGenerators {
  def randomAlphanumeric(length: Int = 8): String =
    Random.alphanumeric take length mkString

  def randomBytes(length: Int = 1024): Array[Byte] = {
    val byteArray = new Array[Byte](length)

    Random.nextBytes(byteArray)

    byteArray
  }

  def randomAlphanumericWithSpace(length: Int = 8): String = {
    val str = randomAlphanumeric(length).toCharArray

    // Randomly choose an index in the string
    // to replace with a space,
    // avoiding the beginning or the end.

    val spaceIndex = Random.nextInt(str.length - 2) + 1
    str.updated(spaceIndex, ' ').toString
  }

  def randomUUID: UUID = UUID.randomUUID()

  def randomInt(from: Int, to: Int): Int = {
    val difference = to - from

    assert(difference > 0)

    val randomOffset = Random.nextInt(difference) + 1

    from + randomOffset
  }

  def randomInstant: Instant =
    Instant.now().plusSeconds(Random.nextInt())
}
