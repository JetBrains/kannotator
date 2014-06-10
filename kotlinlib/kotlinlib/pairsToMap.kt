package kotlinlib

import java.util.LinkedHashMap
import java.util.HashMap

public fun <K, V, M: MutableMap<K, V>> Iterator<Pair<K, V>>.toMutableMap(map: M): M {
    for ((k, v) in this) {
        map.put(k, v)
    }
    return map
}

public fun <K, V> Iterator<Pair<K, V>>.toMutableMap(): MutableMap<K, V> = toMutableMap(LinkedHashMap<K, V>())

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): Map<K, V> = iterator().toMutableMap()

public fun <T, K, V, M: MutableMap<K, V>> Iterable<T>.toMutableMap(map: M, mapper: (T) -> Pair<K, V>): M =
        iterator().map(mapper).toMutableMap(map)

public fun <T, K, V> Iterable<T>.toMap(mapper: (T) -> Pair<K, V>): Map<K, V> = map(mapper).toMap()