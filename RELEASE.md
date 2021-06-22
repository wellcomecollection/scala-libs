RELEASE_TYPE: major

### Libraries affected

`elasticsearch`

### Description

Converts IndexConfig from an `object` to a `case class`.

This is to better reflect the way IndexConfig is used across the code base
i.e. as immutable data structures.

Add some creation ops from type config to IndexConfig.