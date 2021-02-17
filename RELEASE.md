RELEASE_TYPE: patch

If a service using `WellcomeApp` throws a terminal exception, it should actually stop running.
Currently, services hang around forever until they get purged by autoscaling.
