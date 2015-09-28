package kotlinlib

import java.util.Comparator
import java.util.Collections

fun <T> Iterable<T>.sortByToString() = sortedBy { it.toString() }
