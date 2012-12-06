package org.jetbrains.kannotator.annotationsInference.nullability

import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.declarations.ClassName

enum class NullabilityAnnotation : Annotation {
    NOT_NULL
    NULLABLE
}

fun NullabilityValueInfo.toAnnotation() : NullabilityAnnotation? = when (this) {
    NULL, NULLABLE -> NullabilityAnnotation.NULLABLE
    NOT_NULL -> NullabilityAnnotation.NOT_NULL
    CONFLICT, UNKNOWN -> null
}

fun NullabilityAnnotation?.toValueInfo() : NullabilityValueInfo = when (this) {
    NullabilityAnnotation.NOT_NULL -> NOT_NULL
    NullabilityAnnotation.NULLABLE -> NULLABLE
    null -> UNKNOWN
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
