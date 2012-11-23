package org.jetbrains.kannotator.annotationsInference.propagation

import org.jetbrains.kannotator.declarations.Variance
import org.jetbrains.kannotator.declarations.Variance.*
import org.jetbrains.kannotator.declarations.PositionWithinDeclaration

trait AnnotationLattice<A> {
    fun leastCommonUpperBound(a: A, b: A): A
    fun greatestCommonLowerBound(a: A, b: A): A
}

fun <A> AnnotationLattice<A>.unify(position: PositionWithinDeclaration, parent: A, child: A, failOnError: Boolean = true): A {
    return when (position.variance) {
        COVARIANT -> leastCommonUpperBound(parent, child)
        CONTRAVARIANT -> greatestCommonLowerBound(parent, child)
        INVARIANT -> {
            if (failOnError){
                assert(parent == child) {"Conflicting annotations: $parent and $child"}
            }
            child
        }
    }
}

fun <A> AnnotationLattice<A>.subsumes(position: PositionWithinDeclaration, parent: A, child: A): Boolean =
        parent == unify(position, parent, child, false)

fun <A> AnnotationLattice<A>.unify(position: PositionWithinDeclaration, annotations: Collection<out A>): A =
        annotations.reduce {(left, right) -> unify(position, left, right)}

abstract class TwoElementLattice<A>(val small: A, val big: A) : AnnotationLattice<A> {

    override fun greatestCommonLowerBound(a: A, b: A): A {
        return if (a == big && b == big) big else small
    }

    override fun leastCommonUpperBound(a: A, b: A): A {
        return if (a == small && b == small) small else big
    }

}
