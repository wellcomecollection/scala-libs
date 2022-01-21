package weco.storage

sealed trait StorageError {
  val e: Throwable
}

sealed trait CodecError
sealed trait BackendError

sealed trait UpdateError extends StorageError
sealed trait UpdateFunctionError extends UpdateError

case class UpdateNoSourceError(err: NotFoundError) extends UpdateError {
  val e: Throwable = err.e
}
case class UpdateReadError(err: ReadError) extends UpdateError {
  val e: Throwable = err.e
}
case class UpdateWriteError(err: WriteError) extends UpdateError {
  val e: Throwable = err.e
}

case class UpdateNotApplied(e: Throwable) extends UpdateFunctionError
case class UpdateUnexpectedError(e: Throwable) extends UpdateFunctionError

sealed trait WriteError extends StorageError
sealed trait EncoderError extends WriteError

case class StoreWriteError(e: Throwable) extends WriteError with BackendError

case class OverwriteError(e: Throwable) extends WriteError with BackendError

case class IncorrectStreamLengthError(e: Throwable)
    extends DecoderError
    with EncoderError

case class JsonEncodingError(e: Throwable) extends EncoderError

sealed trait ReadError extends StorageError
sealed trait NotFoundError extends ReadError
sealed trait VersionError extends StorageError

case class NoVersionExistsError(message: String)
    extends VersionError
    with NotFoundError {
  val e: Throwable = new Throwable(message)
}

case class HigherVersionExistsError(message: String)
    extends VersionError
    with WriteError {
  val e: Throwable = new Throwable(message)
}

case class VersionAlreadyExistsError(message: String)
    extends VersionError
    with WriteError {
  val e: Throwable = new Throwable(message)
}

case class InvalidIdentifierFailure(e: Throwable) extends WriteError

case class DoesNotExistError(e: Throwable)
    extends NotFoundError
    with BackendError

case class StoreReadError(e: Throwable) extends ReadError with BackendError

case class DanglingHybridStorePointerError(e: Throwable) extends ReadError

case class CannotCloseStreamError(e: Throwable) extends ReadError

sealed trait DecoderError extends ReadError

case class ByteDecodingError(e: Throwable) extends DecoderError

case class StringDecodingError(e: Throwable) extends DecoderError

case class JsonDecodingError(e: Throwable) extends DecoderError

sealed trait MaximaError extends ReadError with BackendError

case class MaximaReadError(e: Throwable) extends MaximaError with StorageError
case class NoMaximaValueError(e: Throwable)
    extends MaximaError
    with StorageError

case class ListingFailure[SrcLocation](source: SrcLocation, e: Throwable)
    extends ReadError
