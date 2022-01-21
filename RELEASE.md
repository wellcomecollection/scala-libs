RELEASE_TYPE: major

This release rolls back some of the SNS error handling logic in v31.0.0, because it was too disruptive downstream and I found a simpler path forward.