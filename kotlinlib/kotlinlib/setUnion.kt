package kotlinlib

import java.util.HashSet

public fun <T> Set<T>.union(other: Collection<T>): Set<T> {
    val result = HashSet(this)
    result.addAll(other)
    return result
}