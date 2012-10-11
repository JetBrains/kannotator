package org.jetbrains.kannotator.declarations

import kotlinlib.join

trait PositionWithinMethod

object RETURN_TYPE : PositionWithinMethod {
    fun toString(): String = "RETURN_TYPE"
}

data class ParameterPosition(val index: Int) : PositionWithinMethod

trait AnnotationPosition

trait MethodTypePosition : AnnotationPosition {
    val method: Method
    val positionWithinMethod: PositionWithinMethod
}

trait FieldTypePosition : AnnotationPosition {
    val field: Field
}

trait AnnotatedType {
    val position: AnnotationPosition
    val arguments: List<AnnotatedType>
}