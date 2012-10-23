package kotlinlib

/**
 * Puts all the entries into this [[MutableMap]] with the first value in the pair being the key and the second the value
 */
public inline fun <K,V> MutableMap<K,V>.putAll(values: Iterable<Pair<K, V>>) {
    for (v in values) {
        put(v.first, v.second)
    }
}





