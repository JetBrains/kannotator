package kotlinlib

import java.util.LinkedHashSet

public fun <T> Collection<T>.subtract(other: Collection<T>): Set<T> {
    val resultSet = LinkedHashSet(this)
    resultSet.removeAll(other)
    return resultSet
}
