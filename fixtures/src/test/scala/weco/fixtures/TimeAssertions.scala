package weco.fixtures

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}

import java.time.Instant
import scala.concurrent.duration._

trait TimeAssertions extends Matchers {
  class InstantMatcher(within: Duration) extends BeMatcher[Instant] {
    override def apply(t: Instant): MatchResult = {
      val interval = Instant.now().toEpochMilli - t.toEpochMilli
      MatchResult(
        interval < within.toMillis,
        s"$t is not recent",
        s"$t is recent"
      )
    }
  }

  def recent(within: Duration = 3 seconds): InstantMatcher =
    new InstantMatcher(within)
}

class TimeAssertionsTest extends AnyFunSpec with Matchers with TimeAssertions {
  it("finds times that are recent") {
    Instant.now() shouldBe recent()
  }

  it("finds times that aren't recent") {
    Instant.now().minusSeconds(5L) should not be recent()
  }
}
