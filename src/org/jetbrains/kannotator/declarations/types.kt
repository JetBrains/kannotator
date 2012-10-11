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

object RETURN_TYPE : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.COVARIANT    
    
    fun toString(): String = "RETURN_TYPE"
}

data class ParameterPosition(val index: Int) : PositionWithinDeclaration {
    override val variance: Variance
        get() = Variance.CONTRAVARIANT    
}

trait AnnotationPosition

trait MethodTypePosition : AnnotationPosition {
    val method: Method
    val positionWithinMethod: PositionWithinDeclaration
}

trait FieldTypePosition : AnnotationPosition {
    val field: Field
}

trait AnnotatedType {
    val position: AnnotationPosition
    val arguments: List<AnnotatedType>
}