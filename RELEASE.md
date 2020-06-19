RELEASE_TYPE: minor

### Libraries affected

`typesafe_app`

### Description

Updates `EnrichConfig` to add:

- `getStringOption`
- `getStringOrElse`
- `requiredString`
- `getIntOption`
- `getIntOrElse`
- `requiredInt`

Deprecates:

- `get[T]`
- `getOrElse[T]`
- `required[T]`

This change is to provide working extraction of optional `Int` & `String` config.