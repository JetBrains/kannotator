package org.jetbrains.kannotator.declarations

import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet
import kotlinlib.union
import java.util.Collections

class AnnotationsImpl<A>(val delegate: Annotations<A>? = null) : MutableAnnotations<A> {

    private val data = HashMap<TypePosition, MutableSet<A>>()

    override fun get(typePosition: TypePosition): Collection<A> {
        val my = data.getOrElse(typePosition) { HashSet(0) }!!
        val theirs = delegate?.get(typePosition) ?: Collections.emptySet<A>()

        return my union theirs
    }

    override fun add(typePosition: TypePosition, annotation: A) {
        data.getOrPut(typePosition, { HashSet(1) }).add(annotation)
    }

}