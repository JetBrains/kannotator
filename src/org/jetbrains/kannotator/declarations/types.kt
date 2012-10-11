package org.jetbrains.kannotator.declarations

import kotlinlib.join

enum class Variance {
    COVARIANT
    CONTRAVARIANT
    INVARIANT
}

trait PositionWithinDeclaration {
    val variance: Variance
}

object FIELD_TYPE : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.INVARIANT
    
    fun toString(): String = "FIELD_TYPE"
}

object RETURN_TYPE : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.COVARIANT

    fun toString(): String = "RETURN_TYPE"
}

data class ParameterPosition(val index: Int) : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.CONTRAVARIANT    
}

trait AnnotationPosition {
    val relativePosition: PositionWithinDeclaration
}

trait MethodTypePosition : AnnotationPosition {
    val method: Method
    override val relativePosition: PositionWithinDeclaration
}

trait FieldTypePosition : AnnotationPosition {
    val field: Field
}

trait AnnotatedType {
    val position: AnnotationPosition
    val arguments: List<AnnotatedType>
}