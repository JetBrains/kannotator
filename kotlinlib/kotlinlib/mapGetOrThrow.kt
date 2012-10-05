package kotlinlib

import java.util.NoSuchElementException

public fun <K, V> Map<K, V>.getOrThrow(key: K): V {
    return getOrElse(key) {
        throw NoSuchElementException("No entry for key $key")
    }
}