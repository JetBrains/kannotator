package org.jetbrains.kannotator.index

import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.TypePosition

trait DeclarationIndex {
    fun findMethod(owner: ClassName, name: String, desc: String) : Method?
//    fun findField(owner: ClassName, name: String) : Method?

//    fun findOverridingMethods(method: Method): Collection<Method>
//    fun findMethodsOverriddenBy(method: Method): Collection<Method>
}

trait AnnotationKeyIndex {
    fun findPositionByAnnotationKeyString(annotationKey: String): TypePosition?
}

