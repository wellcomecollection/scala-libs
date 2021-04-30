package weco.http.models

case class HTTPServerConfig(
  host: String,
  port: Int,
  externalBaseURL: String
)
