package org.jetbrains.kannotator.annotationsInference.propagation

import org.jetbrains.kannotator.declarations.Variance
import org.jetbrains.kannotator.declarations.Variance.*
import org.jetbrains.kannotator.declarations.PositionWithinDeclaration

trait AnnotationLattice<A> {
    fun leastCommonUpperBound(a: A, b: A): A
    fun greatestCommonLowerBound(a: A, b: A): A
}

abstract class TwoElementLattice<A>(val small: A, val big: A) : AnnotationLattice<A> {

    override fun greatestCommonLowerBound(a: A, b: A): A {
        return if (a == big && b == big) big else small
    }

    override fun leastCommonUpperBound(a: A, b: A): A {
        return if (a == small && b == small) small else big
    }

}
