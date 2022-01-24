RELEASE_TYPE: minor

This release tidies up several traits which are only used in the `messaging` library, which were intended to be extensible but in practice only have one implementation.
There should be no effect on downstream users.