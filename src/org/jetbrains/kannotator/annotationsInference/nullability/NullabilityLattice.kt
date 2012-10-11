package org.jetbrains.kannotator.annotationsInference.nullability

import org.jetbrains.kannotator.annotationsInference.nullability.*
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation.*
import org.jetbrains.kannotator.annotationsInference.propagation.AnnotationLattice
import org.jetbrains.kannotator.annotationsInference.propagation.TwoElementLattice

object NullabiltyLattice : TwoElementLattice<NullabilityAnnotation>(
        small = NOT_NULL,
        big = NULLABLE
)