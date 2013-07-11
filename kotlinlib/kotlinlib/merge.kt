package kotlinlib

import java.util.HashMap

fun <K, V, R> mapMerge(
        m1: Map<K, V>, m2: Map<K, V>,
        merger: (key: K, v1: V, v2: V) -> R
): Map<K, R> {
    return mapMergeTo(m1, m2, HashMap<K, R>(), merger)
}

fun <K, V, R, M: MutableMap<K, R>> mapMergeTo(
        m1: Map<K, V>, m2: Map<K, V>,
        resultMap: M,
        merger: (key: K, v1: V, v2: V) -> R
): M {
    for ((key, v1) in m1) {
        val v2 = m2.get(key)
        if (v1 != null && v2 != null) {
            resultMap.put(key, merger(key, v1, v2))
        }
    }

    return resultMap
}