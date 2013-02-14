package kotlinlib

public inline fun <K, V, L, U> Map<K, V>.mapKeysAndValues(
        keyTransform : (Map.Entry<K, V>) -> L,
        valueTransform : (Map.Entry<K, V>) -> U): Map<L, U> {
    return mapKeysAndValuesTo(java.util.HashMap<L, U>(this.size), keyTransform, valueTransform)
}

public inline fun <K, V, L, U, C: MutableMap<L, U>> Map<K, V>.mapKeysAndValuesTo(
        result: C,
        keyTransform : (Map.Entry<K, V>) -> L,
        valueTransform : (Map.Entry<K, V>) -> U) : C {
    for (e in this) {
        result.put(keyTransform(e), valueTransform(e))
    }
    return result
}