RELEASE_TYPE: minor

### Libraries affected

`typesafe_app`

### Description

Updates `EnrichConfig` to add:

- `getStringOption`
- `requiredString`
- `getIntOption`
- `requiredInt`

Deprecates:

- `get[T]`
- `getOrElse[T]`
- `required[T]`

This change is to provide working extraction of optional `Int` & `String` config.
