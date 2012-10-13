package kotlinlib

import java.util.ArrayList
import java.util.HashMap

public fun <T, K> Iterable<T>.classify(bucketId: (T) -> K): Map<K, List<T>> {
    val map = HashMap<K, MutableList<T>>()

    for (item in this) {
        val key = bucketId(item)
        map.getOrPut(key, {ArrayList()}).add(item)
    }
    return map
}