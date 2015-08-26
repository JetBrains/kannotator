package org.jetbrains.kannotator.declarations

interface Annotations<out A: Any> {
    val delegate: Annotations<A>?

    fun get(typePosition: AnnotationPosition): A?
    fun positions(): Set<AnnotationPosition>
    fun forEach(body: (AnnotationPosition, A) -> Unit)
}

interface MutableAnnotations<A : Any> : Annotations<A> {
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