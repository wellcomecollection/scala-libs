package uk.ac.wellcome.json.utils

import io.circe.Json
import org.scalatest.{Assertion, Matchers}
import io.circe.parser._

trait JsonAssertions extends Matchers {

  def assertJsonStringsAreEqual(json1: String, json2: String): Assertion = {
    val tree1 = parseOrElse(json1)
    val tree2 = parseOrElse(json2)
    tree1 shouldBe tree2
  }

  def assertJsonStringsAreDifferent(json1: String, json2: String): Assertion = {
    val tree1 = parseOrElse(json1)
    val tree2 = parseOrElse(json2)
    tree1 shouldNot be(tree2)
  }

  private def parseOrElse(jsonString: String): Json =
    parse(jsonString) match {
      case Right(t) => t
      case Left(err) => throw err
    }
}
