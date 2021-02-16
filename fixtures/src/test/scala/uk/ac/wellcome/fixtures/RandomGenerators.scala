package uk.ac.wellcome.fixtures

import java.time.Instant
import java.util.UUID

import scala.util.Random

trait RandomGenerators {
  def randomAlphanumeric(length: Int = randomInt(from = 5, to = 10)): String =
    Random.alphanumeric take length mkString

  def randomAlphanumericWithSpace(length: Int = 8): String = {
    val str = randomAlphanumeric(length).toCharArray

    // Randomly choose an index in the string
    // to replace with a space,
    // avoiding the beginning or the end.

    val spaceIndex = Random.nextInt(str.length - 2) + 1
    str.updated(spaceIndex, ' ').toString
  }

  def randomBytes(length: Int = 1024): Array[Byte] = {
    val byteArray = new Array[Byte](length)

    Random.nextBytes(byteArray)

    byteArray
  }

  def randomStringOfByteLength(length: Int): String = {
    // Generate bytes within UTF-16 mappable range
    // 0 to 127 maps directly to Unicode code points in the ASCII range
    val chars = (1 to length).map { _ =>
      randomInt(from = 97, to = 122).toByte.toChar
    }

    chars.mkString
  }

  def randomUUID: UUID = UUID.randomUUID()

  def randomInt(from: Int = Int.MinValue, to: Int = Int.MaxValue): Int = {
    val difference = to - from

    assert(difference > 0)

    val randomOffset = Random.nextInt(difference) + 1

    from + randomOffset
  }

  // The slightly unusual default means IntelliJ won't whinge that min=0 is
  // the default when you use it.
  def collectionOf[T](min: Int = chooseFrom(0), max: Int = 10)(
    f: => T): Seq[T] =
    (1 to randomInt(from = min, to = max)).map { _ =>
      f
    }

  def chooseFrom[T](seq: T*): T =
    seq(Random.nextInt(seq.size))

  def randomSample[T](seq: Seq[T], size: Int): Seq[T] =
    Random.shuffle(seq).take(size)

  def randomInstant: Instant =
    Instant.now().plusSeconds(Random.nextInt())
}
