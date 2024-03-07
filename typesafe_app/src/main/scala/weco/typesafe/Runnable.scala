package weco.typesafe

import weco.Logging

import scala.concurrent.Future

trait Runnable extends Logging {
  def run(): Future[Any]
}
