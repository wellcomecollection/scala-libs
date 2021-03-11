package uk.ac.wellcome.elasticsearch.model

trait IndexId[T] {
  def indexId(t: T): String
}

trait Version[T] {
  def version(t: T): Int
}
