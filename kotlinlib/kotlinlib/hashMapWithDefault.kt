package kotlinlib

import java.util.HashMap

public inline fun <K, V> hashMapWithDefault(keys: Iterable<K>, defaultValue: V) : MutableMap<K, V> {
    val hashMap = HashMap<K, V>()
    for (key in keys) {
        hashMap[key] = defaultValue
    }

    return hashMap
}
