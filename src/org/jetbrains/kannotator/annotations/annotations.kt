package org.jetbrains.kannotator.declarations

trait Annotations<out A> {
    fun get(typePosition: TypePosition): A?
    fun forEach(body: (TypePosition, A) -> Unit)
}

trait MutableAnnotations<A> : Annotations<A> {
    fun set(typePosition: TypePosition, annotation: A)
}