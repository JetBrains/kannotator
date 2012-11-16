package org.jetbrains.kannotator.annotationsInference.mutability

import org.jetbrains.kannotator.annotationsInference.Annotation

enum class MutabilityAnnotation : Annotation {
    MUTABLE
    IMMUTABLE
}

private val JB_MUTABLE = "org.jetbrains.kannotator.runtime.annotations.Mutable"
private val JB_IMMUTABLE = "org.jetbrains.kannotator.runtime.annotations.ReadOnly"

fun classNamesToMutabilityAnnotation(canonicalClassNames: Set<String>) : MutabilityAnnotation? {
    val containsMutable = canonicalClassNames.contains(JB_MUTABLE)
    val containsImmutable = canonicalClassNames.contains(JB_IMMUTABLE)

    if (containsMutable == containsImmutable) return null

    return if (containsMutable)
        MutabilityAnnotation.MUTABLE
    else
        MutabilityAnnotation.IMMUTABLE
}