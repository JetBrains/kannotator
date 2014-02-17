### JDK Annotations

This folder contains files for customization on inference of JDK annotations

* `annotations/java`, `annotations/javax` - annotations which were originally developed in integration tests
(incorrect annotations were removed - see `removed.md` for details)
* `annotations/delta.xml` - annotations added later, when comparing annotations distributed with Kotlin
and output of KAnnotator
* `exceptions.txt` - settings for conflict resolution. Annotations for positions listed in this file
will be taken from input annotations even if KAnnotator infers different result.
* `propagationOverrides.txt` - forcing these annotations to be propagated through all hierarchy
* `includedClassNames.txt` - dumping these classes into `annotations.xml` even they are not public

JDK annotations are built via `org.jetbrains.kannotator.client.sdk.SDKPackage`. For automatic build use `sdk_annotations.xml` as the last step.
