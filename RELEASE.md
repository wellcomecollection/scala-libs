RELEASE_TYPE: minor

Remove the AWS CRT libraries, which are causing link issues in the bag replicator in the storage service:

> java.lang.UnsatisfiedLinkError: /tmp/AWSCRT_18127555281524790519libaws-crt-jni.so: Error loading shared library ld-linux-x86-64.so.2: No such file or directory (needed by /tmp/AWSCRT_18127555281524790519libaws-crt-jni.so)
