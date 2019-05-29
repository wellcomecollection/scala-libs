package uk.ac.wellcome.fixtures

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
}
