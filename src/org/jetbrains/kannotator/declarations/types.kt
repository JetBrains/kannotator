package org.jetbrains.kannotator.declarations

import kotlinlib.join

trait TypePosition {

}

trait AnnotatedType {
    val position: TypePosition
    val arguments: List<AnnotatedType>
}