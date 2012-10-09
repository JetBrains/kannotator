package org.jetbrains.kannotator.declarations

import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet
import kotlinlib.union
import java.util.Collections

class AnnotationsImpl<A: Any>(val delegate: Annotations<A>? = null) : MutableAnnotations<A> {
    private val data = HashMap<TypePosition, A>()

    override fun get(typePosition: TypePosition): A? {
        val my = data[typePosition]
        val theirs = delegate?.get(typePosition)

        return if (my != null) my else theirs
    }

    override fun forEach(body: (TypePosition, A) -> Unit) {
        delegate?.forEach(body)
        for ((position, annotation) in data) {
            body(position, annotation)
        }
    }

    override fun set(typePosition: TypePosition, annotation: A) {
        data[typePosition] = annotation
    }
}

public fun <A> MutableAnnotations<A>.setIfNotNull(position: TypePosition, annotation: A?) {
    if (annotation != null) {
        this[position] = annotation
    }
}