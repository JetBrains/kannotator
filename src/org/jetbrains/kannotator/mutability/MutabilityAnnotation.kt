package org.jetbrains.kannotator.mutability

import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.annotationsInference.DerivedAnnotation

enum class MutabilityAnnotation : DerivedAnnotation {
    MUTABLE IMMUTABLE
}

class MutabilityAssert(val shouldBeMutable: Value)