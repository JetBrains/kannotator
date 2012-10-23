package org.jetbrains.kannotator.index

import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method

trait DeclarationIndex {
    fun findMethod(owner: ClassName, name: String, desc: String) : Method?
    fun findField(owner: ClassName, name: String) : Field?

//    fun findOverridingMethods(method: Method): Collection<Method>
//    fun findMethodsOverriddenBy(method: Method): Collection<Method>
}

trait AnnotationKeyIndex {
    fun findPositionByAnnotationKeyString(annotationKey: String): AnnotationPosition?
}

