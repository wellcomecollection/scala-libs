package weco.http.client.sierra

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  OAuth2BearerToken
}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import weco.http.client.{HttpClient, HttpGet, HttpPost}
import weco.http.json.CirceMarshalling

import java.time.Instant
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SierraOauthHttpClient(
  underlying: HttpPost with HttpGet,
  val tokenPath: Path = Path("token"),
  val credentials: BasicHttpCredentials,
  val expiryGracePeriod: Duration = 60.seconds
)(
  implicit
  val system: ActorSystem,
  val ec: ExecutionContext
) extends HttpClient
    with HttpGet
    with HttpPost
    with TokenExchange[BasicHttpCredentials, OAuth2BearerToken] {

  import uk.ac.wellcome.json.JsonUtil._

  implicit val um: FromEntityUnmarshaller[SierraAccessToken] =
    CirceMarshalling.fromDecoder[SierraAccessToken]

  // This implements the Client Credentials flow, as described in the Sierra docs:
  // https://techdocs.iii.com/sierraapi/Content/zReference/authClient.htm
  //
  // We make a request with a client key and secret, retrieve an access token which
  // lasts an hour, and use that for future requests.  When the hour is up, we have
  // to fetch a new token.
  //
  override protected def getNewToken(
    credentials: BasicHttpCredentials
  ): Future[(OAuth2BearerToken, Instant)] =
    for {
      tokenResponse <- underlying.post[Unit](
        path = tokenPath,
        headers = List(Authorization(credentials))
      )

      accessToken <- tokenResponse.status match {
        case StatusCodes.OK => Unmarshal(tokenResponse).to[SierraAccessToken]
        case code =>
          Unmarshal(tokenResponse).to[String].flatMap { resp =>
            Future.failed(
              new Throwable(s"Unexpected status code $code from $tokenPath: $resp")
            )
          }
      }

      result = (
        OAuth2BearerToken(accessToken.accessToken),
        Instant.now().plusSeconds(accessToken.expiresIn)
      )
    } yield result

  override def singleRequest(request: HttpRequest): Future[HttpResponse] =
    for {
      token <- getToken(credentials)

      authenticatedRequest = {
        // We're going to set our own Authorization header on this request
        // using the token, so there shouldn't be one already.
        //
        // Are multiple Authorization headers allowed by HTTP?  It doesn't matter,
        // it's not something we should be doing.
        val existingAuthHeaders = request.headers.collect {
          case auth: Authorization => auth
        }
        require(
          existingAuthHeaders.isEmpty,
          s"HTTP request already has auth headers: $request")

        request.copy(
          headers = request.headers :+ Authorization(token)
        )
      }

      response <- underlying.singleRequest(authenticatedRequest)
    } yield response

  override val baseUri: Uri = underlying.baseUri
}
