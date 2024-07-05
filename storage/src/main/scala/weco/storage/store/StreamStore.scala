package weco.storage.store

import weco.storage.streaming.InputStreamWithLength

trait StreamStore[Ident] extends Store[Ident, InputStreamWithLength]
