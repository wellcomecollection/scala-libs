package weco

import com.typesafe.scalalogging.LazyLogging
import net.logstash.logback.argument.StructuredArguments.entries
import scala.collection.JavaConverters._

trait Logging extends LazyLogging {

  // Error
  def error(message: String): Unit = logger.error(message)
  def error(message: String, cause: Throwable): Unit =
    logger.error(message, cause)
  def error(message: String, event: Map[String, String]): Unit =
    logger.error(message, entries(event.asJava))
  def error(message: String, event: Map[String, String], cause: Throwable): Unit =
    logger.error(message, entries(event.asJava), cause)

  // Warn
  def warn(message: String): Unit = logger.warn(message)
  def warn(message: String, cause: Throwable): Unit =
    logger.warn(message, cause)
  def warn(message: String, event: Map[String, String]): Unit =
    logger.warn(message, entries(event.asJava))
  def warn(message: String, event: Map[String, String], cause: Throwable): Unit =
    logger.warn(message, entries(event.asJava), cause)

  // Info
  def info(message: String): Unit = logger.info(message)
  def info(message: String, cause: Throwable): Unit =
    logger.info(message, cause)
  def info(message: String, event: Map[String, String]): Unit =
    logger.info(message, entries(event.asJava))
  def info(message: String, event: Map[String, String], cause: Throwable): Unit =
    logger.info(message, entries(event.asJava), cause)

  // Debug
  def debug(message: String): Unit = logger.debug(message)
  def debug(message: String, cause: Throwable): Unit =
    logger.debug(message, cause)
  def debug(message: String, event: Map[String, String]): Unit =
    logger.debug(message, entries(event.asJava))
  def debug(message: String, event: Map[String, String], cause: Throwable): Unit =
    logger.debug(message, entries(event.asJava), cause)

  // Trace
  def trace(message: String): Unit = logger.trace(message)
  def trace(message: String, cause: Throwable): Unit =
    logger.trace(message, cause)
  def trace(message: String, args: Any*): Unit =
    logger.trace(message, args)
  def trace(message: String, event: Map[String, String]): Unit =
    logger.trace(message, entries(event.asJava))
  def trace(message: String, event: Map[String, String], cause: Throwable): Unit =
    logger.trace(message, entries(event.asJava), cause)

}