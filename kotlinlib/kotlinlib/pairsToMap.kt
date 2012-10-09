package kotlinlib

import java.util.LinkedHashMap

public fun <K, V> Iterator<Pair<K, V>>.toMutableMap(): MutableMap<K, V> {
    val map = LinkedHashMap<K, V>()
    for ((k, v) in this) {
        map[k] = v
    }
    return map
}

public fun <K, V> Iterator<Pair<K, V>>.toMap(): Map<K, V> = toMutableMap()

public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> = iterator().toMap()

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap(): Map<K, V> = iterator().toMutableMap()