package kotlinlib

import java.util.LinkedHashSet

public fun <T> Collection<T>.intersect(other: Collection<T>): Set<T> {
    val resultSet = LinkedHashSet(this)
    resultSet.retainAll(other)
    return resultSet
}
