package org.jetbrains.kannotator.annotationsInference.mutability

import org.jetbrains.kannotator.annotationsInference.mutability.*
import org.jetbrains.kannotator.annotationsInference.mutability.MutabilityAnnotation.*
import org.jetbrains.kannotator.annotationsInference.propagation.AnnotationLattice
import org.jetbrains.kannotator.annotationsInference.propagation.TwoElementLattice

object MutabiltyLattice : TwoElementLattice<MutabilityAnnotation>(
        small = READ_ONLY,
        big = MUTABLE
)