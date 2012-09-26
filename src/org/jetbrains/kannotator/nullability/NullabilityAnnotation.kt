package org.jetbrains.kannotator.nullability

import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*

enum class NullabilityAnnotation {
    NOT_NULL NULLABLE
}

fun NullabilityValueInfo.toAnnotation() : NullabilityAnnotation? = when (this) {
    NULL, NULLABLE -> NullabilityAnnotation.NULLABLE
    NOT_NULL -> NullabilityAnnotation.NOT_NULL
    CONFLICT, UNKNOWN -> null
}

