package org.jetbrains.kannotator.declarations

import kotlinlib.join

trait PositionWithinMethod

object RETURN_TYPE : PositionWithinMethod {
    fun toString(): String = "RETURN_TYPE"
}

data class ParameterPosition(val index: Int) : PositionWithinMethod

trait TypePosition {
    val method: Method
    val positionWithinMethod: PositionWithinMethod
}

trait AnnotatedType {
    val position: TypePosition
    val arguments: List<AnnotatedType>
}