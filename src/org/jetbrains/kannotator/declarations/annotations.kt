package org.jetbrains.kannotator.declarations

trait Annotations<out A> {
    fun get(typePosition: TypePosition): Collection<A>
}

trait MutableAnnotations<A> : Annotations<A> {
    fun add(typePosition: TypePosition, annotation: A)
}