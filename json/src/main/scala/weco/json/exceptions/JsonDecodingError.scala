package weco.json.exceptions

case class JsonDecodingError(e: Throwable) extends Exception(e.getMessage)
