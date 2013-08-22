package kotlinlib

public inline fun <K, V, U> Map<K, V>.mapValues(
        valueTransform : (K, V) -> U): Map<K, U> {
    return mapValuesTo(java.util.HashMap<K, U>(this.size), valueTransform)
}

public inline fun <K, V, U, C: MutableMap<K, U>> Map<K, V>.mapValuesTo(
        result: C,
        valueTransform : (K, V) -> U) : C {
    for ((key, value) in this) {
        result.put(key, valueTransform(key, value))
    }
    return result
}

public inline fun <K, V, L, U> Map<K, V>.mapKeysAndValues(
        keyTransform : (K, V) -> L,
        valueTransform : (K, V) -> U): Map<L, U> {
    return mapKeysAndValuesTo(java.util.HashMap<L, U>(this.size), keyTransform, valueTransform)
}

public inline fun <K, V, L, U, C: MutableMap<L, U>> Map<K, V>.mapKeysAndValuesTo(
        result: C,
        keyTransform : (K, V) -> L,
        valueTransform : (K, V) -> U) : C {
    for ((key, value) in this) {
        result.put(keyTransform(key, value), valueTransform(key, value))
    }
    return result
}

public fun <K,V,U> MutableMap<K,V>.mapValues(transform: (K,V)-> U)  : Unit {
        mapKeysAndValues({k,_->k}, {k,v-> transform(k,v)})
}
