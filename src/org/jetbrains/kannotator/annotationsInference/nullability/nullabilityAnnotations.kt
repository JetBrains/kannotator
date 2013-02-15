package org.jetbrains.kannotator.annotationsInference.nullability

import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.annotationsInference.propagation.TwoElementLattice

enum class NullabilityAnnotation : Annotation {
    NOT_NULL
    NULLABLE
}

public val JB_NOT_NULL: String = "org.jetbrains.annotations.NotNull"
public val JB_NULLABLE: String = "org.jetbrains.annotations.Nullable"
public val JSR_305_NOT_NULL: String = "javax.annotation.Nonnull"
public val JSR_305_NULLABLE: String = "javax.annotation.Nullable"

fun classNamesToNullabilityAnnotation(canonicalClassNames: Set<String>) : NullabilityAnnotation? {
    val containsNotNull = canonicalClassNames.contains(JB_NOT_NULL) || canonicalClassNames.contains(JSR_305_NOT_NULL)
    val containsNullable = canonicalClassNames.contains(JB_NULLABLE) || canonicalClassNames.contains(JSR_305_NULLABLE)

    if (containsNotNull == containsNullable) return null
    return if (containsNotNull)
        NullabilityAnnotation.NOT_NULL
    else
        NullabilityAnnotation.NULLABLE
}

object NullabiltyLattice : TwoElementLattice<NullabilityAnnotation>(
        small = NullabilityAnnotation.NOT_NULL,
        big = NullabilityAnnotation.NULLABLE
)