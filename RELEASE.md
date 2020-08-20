RELEASE_TYPE: patch

Warn that listing the contents of an S3/Azure prefix that doesn't end in a slash may include unexpected objects.

e.g. listing `bags/v1` will return everything under `bags/v1/`, but also `bags/v10/`.

This may be the desired behaviour, so listing a prefix that doesn't end with a slash isn't forbidden, but hopefully this makes it easier to debug if weird things are happening.
