package org.jetbrains.kannotator.declarations

trait Annotations<out A: Any> {
    fun get(typePosition: AnnotationPosition): A?
    fun forEach(body: (AnnotationPosition, A) -> Unit)
}

trait MutableAnnotations<A> : Annotations<A> {
    fun set(typePosition: AnnotationPosition, annotation: A)
}

public fun <A: Any> MutableAnnotations<A>.setIfNotNull(position: AnnotationPosition, annotation: A?) {
    if (annotation != null) {
        this[position] = annotation
    }
}

public fun <A: Any> MutableAnnotations<A>.copyAllChanged(
        annotations: Annotations<A>,
        merger: (pos: AnnotationPosition, previous: A?, new: A) -> A = { pos, previous, new -> new }) {
    annotations.forEach { pos, ann ->
        val previous = this[pos]
        if (previous != ann) {
            this[pos] = merger(pos, previous, ann)
        }
    }
}