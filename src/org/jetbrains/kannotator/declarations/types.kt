package org.jetbrains.kannotator.declarations

import kotlinlib.join

trait PositionWithinDeclaration

object RETURN_TYPE : PositionWithinDeclaration {
    fun toString(): String = "RETURN_TYPE"
}

data class ParameterPosition(val index: Int) : PositionWithinDeclaration

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