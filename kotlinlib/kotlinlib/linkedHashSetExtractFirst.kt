package kotlinlib

import java.util.LinkedHashSet

fun <T> LinkedHashSet<T>.removeFirst(): T {
    val iterator = iterator()
    val r = iterator.next()
    iterator.remove()
    return r
}