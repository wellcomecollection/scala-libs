package weco

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.Marker

trait Logging extends LazyLogging {

  // Error
  def error(message: String): Unit = logger.error(message)
  def error(message: String, cause: Throwable): Unit =
    logger.error(message, cause)
  def error(marker: Marker, message: String): Unit =
    logger.error(marker, message)
  def error(marker: Marker, message: String, cause: Throwable): Unit =
    logger.error(marker, message, cause)
  def error(marker: Marker, message: String, args: Any*): Unit =
    logger.error(marker, message, args)

  // Warn
  def warn(message: String): Unit = logger.warn(message)
  def warn(message: String, cause: Throwable): Unit =
    logger.warn(message, cause)
  def warn(marker: Marker, message: String): Unit =
    logger.warn(marker, message)
  def warn(marker: Marker, message: String, cause: Throwable): Unit =
    logger.warn(marker, message, cause)
  def warn(marker: Marker, message: String, args: Any*): Unit =
    logger.warn(marker, message, args)

  // Info
  def info(message: String): Unit = logger.info(message)
  def info(message: String, cause: Throwable): Unit =
    logger.info(message, cause)
  def info(marker: Marker, message: String): Unit =
    logger.info(marker, message)
  def info(marker: Marker, message: String, cause: Throwable): Unit =
    logger.info(marker, message, cause)
  def info(marker: Marker, message: String, args: Any*): Unit =
    logger.info(marker, message, args)

  // Debug
  def debug(message: String): Unit = logger.debug(message)
  def debug(message: String, cause: Throwable): Unit =
    logger.debug(message, cause)
  def debug(marker: Marker, message: String): Unit =
    logger.debug(marker, message)
  def debug(marker: Marker, message: String, cause: Throwable): Unit =
    logger.debug(marker, message, cause)
  def debug(marker: Marker, message: String, args: Any*): Unit =
    logger.debug(marker, message, args)

  // Trace
  def trace(message: String): Unit = logger.trace(message)
  def trace(message: String, cause: Throwable): Unit =
    logger.trace(message, cause)
  def trace(message: String, args: Any*): Unit =
    logger.trace(message, args)
  def trace(marker: Marker, message: String): Unit =
    logger.trace(marker, message)
  def trace(marker: Marker, message: String, cause: Throwable): Unit =
    logger.trace(marker, message, cause)
  def trace(marker: Marker, message: String, args: Any*): Unit =
    logger.trace(marker, message, args)

}