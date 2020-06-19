RELEASE_TYPE: minor

### Libraries affected

`typesafe_app`

### Description

Updates `EnrichConfig` to add:

- `getStringOption`
- `requireString`
- `getIntOption`
- `requireInt`

Deprecates:

- `get[T]`
- `getOrElse[T]`
- `required[T]`

This change is to provide working extraction of optional `Int` & `String` config.
