RELEASE_TYPE: patch

Disallow using `.` and `..` in an S3ObjectLocation key and an AzureBlobLocation name.

In a filesystem, these entries mean "current directory" and "parent directory", but object stores like S3 and Azure Blob Storage aren't filesystems.
These characters cause issues in the console, and their semantics aren't the same as in regular filesystems.

e.g. on a filesystem, `alfa/./bravo` is equivalent to `alfa/bravo`, but these could be two distinct objects in S3

In general we never expect to be creating objects that include these entries in the path, so for now outright block them rather than trying to handle them.
**We can review this limitation if we need to support such keys, but we should consider the semantics carefully.**
