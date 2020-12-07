RELEASE_TYPE: major

This release changes the behaviour of Maxima to return an `Identified[_, _]`, rather than a single value.
This reduces doing double-lookups if you want to get the max version of something in a store, and retrieve the whole record rather than just the max version.

The practical upshot for our code is that if you call `getLatest(…)`, now you're making a single request to DynamoDB rather than two.