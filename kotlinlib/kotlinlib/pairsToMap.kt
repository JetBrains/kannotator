package kotlinlib

import java.util.HashMap

public fun <K, V> Iterator<Pair<K, V>>.toMap(): Map<K, V> {
    val map = HashMap<K, V>()
    for ((k, v) in this) {
        map[k] = v
    }
    return map
}

public fun <K, V> Iterable<Pair<K, V>>.toMap(): Map<K, V> = iterator().toMap()