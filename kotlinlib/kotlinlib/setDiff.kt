package kotlinlib

import java.util.HashSet

public fun <T> Set<T>.minus(other: Collection<T>): Set<T> {
    val result = HashSet(this)
    result.removeAll(other)
    return result
}