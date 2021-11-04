RELEASE_TYPE: patch

This patch adds a dedicated exception `LargeStreamReaderCannotReadRange` to the LargeStreamReader.

If the large stream reader is unable to read a particular chunk, it exposes all the relevant information so an upstream caller can react appropriately (e.g. providing a more user-friendly error message).