package kotlinlib


public data class IndexedElement<T>(
        public val index: Int,
        public val value: T
)

public inline val <T> Array<T>.indexed: Iterator<IndexedElement<T>>
        get() {
            val array = this
            return object : Iterator<IndexedElement<T>> {
                var index = 0
                val maxIndex = array.size - 1

                override fun next(): IndexedElement<T> {
                    return IndexedElement(index, array[index++])
                }

                override fun hasNext(): Boolean {
                    return index <= maxIndex
                }
            }
        }

