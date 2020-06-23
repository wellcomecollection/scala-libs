RELEASE_TYPE: major

### Libraries affected

`storage`

### Description

Roll back all the changes in v12.0.0.  We do want to split `ObjectLocation` eventually, but doing it in one fell swoop is far too big a patch (and blocks other changes to these libraries).
