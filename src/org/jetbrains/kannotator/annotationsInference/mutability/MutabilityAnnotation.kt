package org.jetbrains.kannotator.mutability

import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.Assert

enum class MutabilityAnnotation : Annotation {
    MUTABLE IMMUTABLE
}