package weco.http.monitoring

import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, ETag}
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, Uri}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class WellcomeHttpLoggerTest extends AnyFunSpec with Matchers {
  describe("logging the request") {
    it("doesn't log any headers if none are approved") {
      val logger = new WellcomeHttpLogger(
        requestHeaders = Set(),
        responseHeaders = Set()
      )

      val log = logger.createLogLine(
        request = HttpRequest(
          method = HttpMethods.GET,
          uri = Uri("http://example.net/api/v1/shapes"),
          headers = List(
            ETag("123456789"),
            Authorization(BasicHttpCredentials("secretValue")),
          ),
        ),
        response = "<unknown>"
      )

      log shouldBe """Request: GET /api/v1/shapes / Response: <unknown>"""
    }

    it("only logs the approved headers") {
      val logger = new WellcomeHttpLogger(
        requestHeaders = Set("ETag"),
        responseHeaders = Set()
      )

      val log = logger.createLogLine(
        request = HttpRequest(
          method = HttpMethods.GET,
          uri = Uri("http://example.net/api/v1/shapes"),
          headers = List(
            ETag("123456789"),
            Authorization(BasicHttpCredentials("secretValue")),
          ),
        ),
        response = "<unknown>"
      )

      log shouldBe """Request: GET /api/v1/shapes; ETag="123456789" / Response: <unknown>"""
    }

    it("logs a non-trivial entity, if present") {
      val logger = new WellcomeHttpLogger(
        requestHeaders = Set("ETag"),
        responseHeaders = Set()
      )

      val log = logger.createLogLine(
        request = HttpRequest(
          method = HttpMethods.GET,
          uri = Uri("http://example.net/api/v1/shapes"),
          headers = List(
            ETag("123456789"),
            Authorization(BasicHttpCredentials("secretValue")),
          ),
          entity = HttpEntity("this is my entity")
        ),
        response = "<unknown>"
      )

      log shouldBe """Request: GET /api/v1/shapes; ETag="123456789"; HttpEntity.Strict(text/plain; charset=UTF-8,17 bytes total) / Response: <unknown>"""
    }
  }
}
