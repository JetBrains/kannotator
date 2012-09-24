package org.jetbrains.kannotator.controlFlow

import java.util.HashMap

public class DataKey<in H, out V: Any>

public trait DataHolder<T : DataHolder<T>> {
    fun get<V : Any>(key: DataKey<T, V>): V?
    fun set<V : Any>(key: DataKey<T, V>, value: V)
}

public abstract class DataHolderImpl<T : DataHolder<T>> : DataHolder<T> {
    private val map: MutableMap<DataKey<T, *>, Any> = HashMap()

    override fun <V: Any> get(key: DataKey<T, V>): V? {
        val r = map[key]
        if (r == null) return null
        return r as V
    }

    override fun <V: Any> set(key: DataKey<T, V>, value: V) {
        map[key] = value
    }
}