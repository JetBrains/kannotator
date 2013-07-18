package kotlinlib

fun <T: Any> MutableCollection<T>.addNotNull(item: T?) {
    if (item != null) add(item)
}