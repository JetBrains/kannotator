package kotlinlib


public data class IndexedElement<T>(
        public val index: Int,
        public val value: T
)

public fun <T, C> indexedIterator(collection: C, size: Int, get: (C, Int) -> T): Iterator<IndexedElement<T>> {
    return object : Iterator<IndexedElement<T>> {
        var index = 0
        val maxIndex = size - 1

        override fun next(): IndexedElement<T> {
            return IndexedElement(index, get(collection, index++))
        }

        override fun hasNext(): Boolean {
            return index <= maxIndex
        }
    }
}

public inline val <T> Array<out T>.indexed: Iterator<IndexedElement<T>>
        get() = indexedIterator(this, this.size) { c, i -> c[i] }

public inline val <T> Iterator<T>.indexed: Iterator<IndexedElement<T>>
    get() {
        val iterator = this
        return object : Iterator<IndexedElement<T>> {
            var index = 0

            override fun next(): IndexedElement<T> {
                val r = IndexedElement(index, iterator.next())
                index++
                return r
            }

            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }
        }
    }

public inline val <T> List<T>.indexed: Iterator<IndexedElement<T>>
    get() = iterator().indexed
