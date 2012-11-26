package org.jetbrains.kannotator.declarations

trait Annotations<out A: Any> {
    fun get(typePosition: AnnotationPosition): A?
    fun forEach(body: (AnnotationPosition, A) -> Unit)
}

trait MutableAnnotations<A> : Annotations<A> {
    fun set(typePosition: AnnotationPosition, annotation: A)
}