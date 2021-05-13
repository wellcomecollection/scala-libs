RELEASE_TYPE: minor

Add a TypedString type for times when we want to use a type in place of a string, but we still want the value to serialise as a string in JSON/DynamoDB.

We already have code for this in various places; this consolidates it into one place.