package weco.fixtures

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import java.time.{Duration, Instant}

trait TimeAssertions extends Matchers {
  def assertRecent(instant: Instant, recentSeconds: Int = 1): Assertion =
    Duration
      .between(instant, Instant.now)
      .getSeconds should be <= recentSeconds.toLong

  def assertAllRecent(instants: Seq[Instant], recentSeconds: Int = 1): Unit =
    instants.foreach(i => assertRecent(i, recentSeconds))
}
