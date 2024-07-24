package weco.http

import org.apache.pekko.http.scaladsl.marshalling.Marshal
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.{
  MalformedRequestContentRejection,
  RejectionHandler,
  Route
}
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.util.ByteString
import io.circe.CursorOp
import weco.http.models.DisplayError
import weco.http.monitoring.HttpMetrics

import scala.concurrent.ExecutionContext

trait WellcomeRejectionHandler extends ErrorDirectives {
  import com.github.pjfanning.pekkohttpcirce.ErrorAccumulatingCirceSupport._

  val httpMetrics: HttpMetrics

  implicit val ec: ExecutionContext
  implicit val rejectionHandler: RejectionHandler = buildRejectionHandler()

  private def buildRejectionHandler(): RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(_, causes: DecodingFailures) =>
          handleDecodingFailures(causes)
      }
      .result()
      .seal
      .mapRejectionResponse {
        case res @ HttpResponse(
              statusCode,
              _,
              HttpEntity.Strict(contentType, _),
              _
            ) if contentType != ContentTypes.`application/json` =>
          transformToJsonErrorResponse(statusCode, res)
        case x => x
      }
      .mapRejectionResponse { resp: HttpResponse =>
        httpMetrics.sendMetric(resp)
        resp
      }

  private def handleDecodingFailures(
    causes: DecodingFailures
  ): Route = {
    val message = causes.failures.map { cause =>
      val path = CursorOp.opsToPath(cause.history)

      // Error messages returned by Circe are somewhat inconsistent and we also return our
      // own error messages when decoding enums (DisplayIngestType and DisplayStorageProvider).
      val reason = cause.message match {
        // "Attempt to decode value on failed cursor" seems to mean in circeworld
        // that a required field was not present.
        case s if s.contains("Attempt to decode value on failed cursor") =>
          "required property not supplied."
        case s => s
      }

      s"Invalid value at $path: $reason"
    }

    invalidRequest(description = message.toList.mkString("\n"))
  }

  private def transformToJsonErrorResponse(
    statusCode: StatusCode,
    response: HttpResponse
  ): HttpResponse = {

    val errorResponseMarshallingFlow = Flow[ByteString]
      .mapAsync(parallelism = 1)(data => {
        val description = data.utf8String
        if (statusCode.intValue() >= 500) {
          val response = DisplayError(statusCode = statusCode)
          Marshal(response).to[MessageEntity]
        } else {
          val response =
            DisplayError(
              statusCode = statusCode,
              description = description
            )
          Marshal(response).to[MessageEntity]
        }
      })
      .flatMapConcat(_.dataBytes)

    response
      .transformEntityDataBytes(errorResponseMarshallingFlow)
      .mapEntity(
        entity => entity.withContentType(ContentTypes.`application/json`)
      )
  }
}
