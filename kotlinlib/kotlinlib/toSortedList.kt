package kotlinlib

import java.util.Collections
import java.util.ArrayList

fun <T> Collection<T>.toSortedList(comp: (T, T) -> Int): List<T> {
    val list = ArrayList(this)
    Collections.sort(list, comparator(comp))
    return list
}