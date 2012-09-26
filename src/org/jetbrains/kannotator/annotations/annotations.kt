package org.jetbrains.kannotator.declarations

trait Annotations<out A> {
    fun get(typePosition: TypePosition): Collection<A>
    fun forEach(body: (TypePosition, A) -> Unit)
}

trait MutableAnnotations<A> : Annotations<A> {
    fun add(typePosition: TypePosition, annotation: A)
}