package org.jetbrains.kannotator.annotationsInference.nullability

import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityValueInfo.*

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

private val NULLABILITY_ANNOTATION_CLASSES = hashMap(
        "org.jetbrains.annotations.NotNull" to NullabilityAnnotation.NOT_NULL,
        "org.jetbrains.annotations.Nullable" to NullabilityAnnotation.NULLABLE
)

fun classNameToNullabilityAnnotation(className: String) : NullabilityAnnotation? =
        NULLABILITY_ANNOTATION_CLASSES[className]
