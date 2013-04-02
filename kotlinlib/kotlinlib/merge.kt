package kotlinlib

import java.util.HashMap

fun <K, V, R> mapMerge(
        m1: Map<K, V>, m2: Map<K, V>, keys: Iterable<K>,
        merger: (key: K, v1: V, v2: V) -> R
): Map<K, R> {
    return mapMergeTo(m1, m2, keys, HashMap<K, R>(), merger)
}

fun <K, V, R, M: MutableMap<K, R>> mapMergeTo(
        m1: Map<K, V>, m2: Map<K, V>, keys: Iterable<K>,
        resultMap: M,
        merger: (key: K, v1: V, v2: V) -> R
): M {
    for (key in keys) {
        resultMap.put(key, merger(key, m1.get(key)!!, m2.get(key)!!))
    }

    return resultMap
}