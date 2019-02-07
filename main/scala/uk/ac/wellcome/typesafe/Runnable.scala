package uk.ac.wellcome.typesafe

import scala.concurrent.Future

trait Runnable {
  def run(): Future[Any]
}
