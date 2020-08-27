package uk.ac.wellcome.typesafe

import grizzled.slf4j.Logging

import scala.concurrent.Future

trait Runnable extends Logging {
  def run(): Future[Any]
}
