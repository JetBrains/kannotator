package org.jetbrains.kannotator.declarations

import kotlinlib.join

trait PositionWithinMethod

object RETURN_TYPE : PositionWithinMethod
data class ParameterPosition(val index: Int) : PositionWithinMethod

trait TypePosition {
    val method: Method
    val positionWithinMethod: PositionWithinMethod
}

trait AnnotatedType {
    val position: TypePosition
    val arguments: List<AnnotatedType>
}