package org.jetbrains.kannotator.annotationsInference.propagation

import org.jetbrains.kannotator.declarations.Variance
import org.jetbrains.kannotator.declarations.Variance.*
import org.jetbrains.kannotator.declarations.PositionWithinDeclaration

trait AnnotationLattice<A> {
    fun leastCommonUpperBound(a: A, b: A): A
    fun greatestCommonLowerBound(a: A, b: A): A
}

fun <A> AnnotationLattice<A>.unify(position: PositionWithinDeclaration, parent: A, child: A): A {
    return when (position.variance) {
        COVARIANT -> leastCommonUpperBound(parent, child)
        CONTRAVARIANT -> greatestCommonLowerBound(parent, child)
        INVARIANT -> {
            assert(parent == child) {"Conflicting annotations: $parent and $child"}
            child
        }
    }
}

fun <A> AnnotationLattice<A>.unify(position: PositionWithinDeclaration, annotations: Collection<A>): A =
        annotations.reduce {(left, right) -> unify(position, left, right)}

abstract class TwoElementLattice<A>(val small: A, val big: A) : AnnotationLattice<A> {

    override fun greatestCommonLowerBound(a: A, b: A): A {
        return if (a == big && b == big) big else small
    }

    override fun leastCommonUpperBound(a: A, b: A): A {
        return if (a == small && b == small) small else big
    }

}
