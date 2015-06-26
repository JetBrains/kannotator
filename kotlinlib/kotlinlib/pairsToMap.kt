package kotlinlib

import java.util.LinkedHashMap

private fun <K, V, M: MutableMap<K, V>> Iterator<Pair<K, V>>.toMutableMap(map: M): M {
    for ((k, v) in this) {
        map.put(k, v)
    }
    return map
}

private fun <K, V> Iterator<Pair<K, V>>.toMutableMap(): MutableMap<K, V> = toMutableMap(LinkedHashMap<K, V>())

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): MutableMap<K, V> = iterator().toMutableMap()

public fun <T, K, V, M: MutableMap<K, V>> Iterable<T>.toMutableMap(map: M, mapper: (T) -> Pair<K, V>): M =
        asSequence().map(mapper).iterator().toMutableMap(map)
