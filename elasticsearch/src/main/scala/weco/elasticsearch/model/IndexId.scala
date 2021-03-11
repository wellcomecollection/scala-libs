package weco.elasticsearch.model

trait IndexId[T] {
  def indexId(t: T): String
}
