package org.jetbrains.kannotator.controlFlow.builder.analysis.mutability

import org.jetbrains.kannotator.controlFlow.builder.analysis.Annotation
import org.jetbrains.kannotator.annotationsInference.propagation.TwoElementLattice
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation.*

enum class MutabilityAnnotation : Annotation {
    MUTABLE
    READ_ONLY
}

public val JB_MUTABLE: String = "org.jetbrains.kannotator.runtime.annotations.Mutable"
public val JB_READ_ONLY: String = "org.jetbrains.kannotator.runtime.annotations.ReadOnly"

fun classNamesToMutabilityAnnotation(canonicalClassNames: Set<String>) : MutabilityAnnotation? {
    val containsMutable = canonicalClassNames.contains(JB_MUTABLE)
    val containsImmutable = canonicalClassNames.contains(JB_READ_ONLY)

    if (containsMutable == containsImmutable) return null

    return if (containsMutable)
        MutabilityAnnotation.MUTABLE
    else
        MutabilityAnnotation.READ_ONLY
}

object MutabiltyLattice : TwoElementLattice<MutabilityAnnotation>(
        small = READ_ONLY,
        big = MUTABLE
)