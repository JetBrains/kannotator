package kotlinlib

import java.util.ArrayList

public fun <T, R> Iterator<T>.mapWithIndex(f: (index: Int, value: T) -> R): Iterator<R> {
    return object : Iterator<R> {
        var index = 0

        public override fun hasNext(): Boolean {
            return this@mapWithIndex.hasNext()
        }

        public override fun next(): R {
            val r = f(index, this@mapWithIndex.next())
            index++
            return r
        }
    }
}

public fun <T, R> List<T>.mapWithIndex(f: (index: Int, value: T) -> R): List<R> {
    val result = ArrayList<R>(this.size())
    for (index in this.indices) {
        result.add(f(index, this[index]))
    }
    return result
}

public fun <T, R> Array<T>.mapWithIndex(f: (index: Int, value: T) -> R): List<R> {
    val result = ArrayList<R>(this.size())
    for (i in this.indices) {
        result.add(f(i, this[i]))
    }
    return result
}