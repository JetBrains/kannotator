package kotlinlib

import java.util.NoSuchElementException

public fun <K, V> Map<K, V>.getOrThrow(key: K, message: String = "No entry for key $key"): V {
    return getOrElse(key) {
        throw NoSuchElementException(message)
    }
}