package weco.http.client

import java.time.Instant
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait TokenExchange[C, T] {
  private var cachedToken: Option[(T, Instant)] = None

  // How many seconds before the token expires should we go back and
  // fetch a new token?
  val expiryGracePeriod: Duration

  implicit val ec: ExecutionContext

  protected def getNewToken(credentials: C): Future[(T, Instant)]

  def getToken(credentials: C, forceRefresh: Boolean = false): Future[T] =
    (cachedToken, forceRefresh) match {
      case (Some((token, expiryTime)), false)
          if expiryTime
            .minusSeconds(expiryGracePeriod.toSeconds)
            .isAfter(Instant.now()) =>
        Future.successful(token)

      case _ =>
        getNewToken(credentials).map {
          case (token, expiryTime) =>
            cachedToken = Some((token, expiryTime))
            token
        }
    }
}
