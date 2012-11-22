package org.jetbrains.kannotator.declarations

import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet
import kotlinlib.union
import java.util.Collections

class AnnotationsImpl<A: Any>(val delegate: Annotations<A>? = null) : MutableAnnotations<A> {
    private val data = HashMap<AnnotationPosition, A>()

    override fun get(typePosition: AnnotationPosition): A? {
        val my = data[typePosition]
        val theirs = delegate?.get(typePosition)

        return if (my != null) my else theirs
    }

    override fun forEach(body: (AnnotationPosition, A) -> Unit) {
        delegate?.forEach(body)
        for ((position, annotation) in data) {
            body(position, annotation)
        }
    }

    override fun set(typePosition: AnnotationPosition, annotation: A) {
        data[typePosition] = annotation
    }
}

public fun <A> MutableAnnotations<A>.setIfNotNull(position: AnnotationPosition, annotation: A?) {
    if (annotation != null) {
        this[position] = annotation
    }
}

public fun <A> MutableAnnotations<A>.copyAllChanged(
        annotations: Annotations<A>,
        merger: (pos: AnnotationPosition, previous: A?, new: A) -> A = { pos, previous, new -> new }) {
    annotations.forEach { pos, ann ->
        val previous = this[pos]
        if (previous != ann) {
            this[pos] = merger(pos, previous, ann)
        }
    }
}