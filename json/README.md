# json

Common JSON serialisation & de-serialisation.

-   `JsonUtil`: Wrappers around Circe
-   `JsonAssertions`: Test helpers for checking if two JSON strings are equal or
    different.

This library is used as part of the [Wellcome Digital Platform][platform].

[platform]: https://github.com/wellcomecollection/platform

## Installation

This library is only published to a private S3 bucket.

Wellcome projects have access to this S3 bucket -- you can use our build
scripts to publish a copy to your own private package repository, or vendor
the library by copying the code into your own repository.

Read [the changelog](CHANGELOG.md) to find the latest version.
