RELEASE_TYPE: minor

Remove the `checkForExisting` flag from Transfer and TransferPrefix.

These classes will now *always* check for an existing object before overwriting it.