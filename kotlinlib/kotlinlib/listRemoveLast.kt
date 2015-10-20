package kotlinlib

fun <T> MutableList<T>.removeLast(): T = removeAt(lastIndex)