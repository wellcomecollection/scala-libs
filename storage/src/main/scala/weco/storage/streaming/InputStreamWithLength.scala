package weco.storage.streaming

import java.io.{FilterInputStream, InputStream}

class InputStreamWithLength(inputStream: InputStream, val length: Long)
    extends FilterInputStream(inputStream)
