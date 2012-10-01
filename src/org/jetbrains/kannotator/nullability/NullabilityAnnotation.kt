package org.jetbrains.kannotator.nullability

import org.jetbrains.kannotator.nullability.NullabilityValueInfo.*
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.AnnotationKind

class Nullability: AnnotationKind

enum class NullabilityAnnotation : Annotation<Nullability> {
    NOT_NULL NULLABLE
}

fun NullabilityValueInfo.toAnnotation() : NullabilityAnnotation? = when (this) {
    NULL, NULLABLE -> NullabilityAnnotation.NULLABLE
    NOT_NULL -> NullabilityAnnotation.NOT_NULL
    CONFLICT, UNKNOWN -> null
}

