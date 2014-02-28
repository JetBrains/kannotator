# Android SDK annotations

Android SDK annotations are inferred in the same way as JDK annotations with minor subtle difference.

We would like to have as few conflicts with annotations supplied with Android Studio as possible.
For this reason, we remove annotations that conflict with Studio annotations (see `androidCombine.kt`).
Unfortunately, after this removal hierarchy is broken. Fortunately, a number of such broken annotations is
small, so it is possible to fix it via manual removal of such broken annotations. This is what file `exclude.xml` for.
