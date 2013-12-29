### JDK Annotations

This folder contains files for customization on inference of JDK annotations

* `jdk-annotations/java`, `jdk-annotations/javax` - annotations which were originally developed in integration tests
* `jdk-annotations/delta.xml` - annotations added later, when comparing annotations distributed with Kotlin
and output of Kannotator
* `exceptions.txt` - settings for conflict resolution. Annotations for positions listed in this file
will be taken from input annotations even if KAnnotator infers different result.
* `propagationOverrides.txt` - forcing these annotations to be propagated through all hierarchy

JDK annotations are built via `org.jetbrains.kannotator.client.jdk.JDKPackage`. The result will be in the folder
`jdk-annotations-snapshot`.

For automatic build use `build_jdk_annotations.xml` as the last step. Annotations will be in `out/kotlin-jdk-annotations.jar`

For now positions in output files are not sorted, so there is a script `sort.sh` which normalizes annotations.
It may be useful for "manual" comparison of previous result with the current one.
