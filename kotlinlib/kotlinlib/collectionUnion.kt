package kotlinlib

import java.util.LinkedHashSet

public fun <T> Collection<T>.union(other: Collection<T>): Set<T> {
    val resultSet = LinkedHashSet(this)
    resultSet.addAll(other)
    return resultSet
}
