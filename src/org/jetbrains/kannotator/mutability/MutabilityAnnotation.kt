package org.jetbrains.kannotator.mutability

import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.AnnotationKind
import org.jetbrains.kannotator.annotationsInference.Assert

class Mutability: AnnotationKind
enum class MutabilityAnnotation : Annotation<Mutability> {
    MUTABLE IMMUTABLE
}