package org.jetbrains.kannotator.declarations

import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet
import kotlinlib.union
import java.util.Collections

class AnnotationsImpl<A: Any>(val delegate: Annotations<A>? = null) : MutableAnnotations<A> {
    private val data = HashMap<AnnotationPosition, A>()

    override fun get(typePosition: AnnotationPosition): A? {
        return data[typePosition] ?: delegate?.get(typePosition)
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