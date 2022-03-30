package weco.http.client

import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpRequest,
  HttpResponse
}
import akka.util.ByteString
import io.circe.Json
import io.circe.parser.parse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class MemoryHttpClient(
  responses: Seq[(HttpRequest, HttpResponse)]
)(
  implicit val ec: ExecutionContext
) extends HttpClient {

  private val iterator = responses.toIterator

  override def singleRequest(request: HttpRequest): Future[HttpResponse] =
    Future {
      val (nextReq, nextResp) = Try { iterator.next() } match {
        case Success((req, resp)) => (req, resp)
        case Failure(err) =>
          throw new RuntimeException(
            s"No more requests expected, but got $request ($err)"
          )
      }

      // These checks all amount to "nextReq != request", but the specific
      // checks are meant to make it easier to debug particular issues.
      if (nextReq.method != request.method) {
        throw new RuntimeException(
          s"Expected request with method ${nextReq.method}, got method with URI ${request.method}")
      }

      if (nextReq.uri != request.uri) {
        throw new RuntimeException(
          s"Expected request with URI ${nextReq.uri}, got request with URI ${request.uri}")
      }

      if (nextReq.headers != request.headers) {
        throw new RuntimeException(
          s"Expected request with headers ${nextReq.headers}, got request with headers ${request.headers}")
      }

      if (!areEquivalent(nextReq.entity, request.entity)) {
        val str1 = getEntityString(nextReq.entity)
        val str2 = getEntityString(request.entity)
        throw new RuntimeException(
          s"Requests have different entities: $str1 / $str2")
      }

      nextResp
    }

  private def getEntityString(entity: HttpEntity): String =
    entity match {
      case HttpEntity.Strict(ContentTypes.`application/json`, json) =>
        parse(json.utf8String).right.get.spaces2
      case HttpEntity.Strict(_, content) => content.utf8String
      case _                             => "<streaming entity>"
    }

  private def areEquivalent(e1: HttpEntity, e2: HttpEntity): Boolean = {
    (e1, e2) match {
      case (entity1, entity2) if entity1 == entity2 => true
      case (
          HttpEntity.Strict(ContentTypes.`application/json`, json1),
          HttpEntity.Strict(ContentTypes.`application/json`, json2))
          if parseOrElse(json1) == parseOrElse(json2) =>
        true

      case _ => false
    }
  }

  private def parseOrElse(json: ByteString): Json =
    parse(new String(json.toArray[Byte])) match {
      case Right(t) => t
      case Left(err) =>
        println(s"Error trying to parse string <<$json>>")
        throw err
    }
}
