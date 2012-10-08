package kotlinlib

import java.util.Collections

//todo rename other to 'arrayListOf', 'linkedListOf'
public fun <T> listOf(vararg values: T): List<T> = arrayList(*values)

//not vals for consistency with map
public fun emptyList(): List<Nothing> = Collections.emptyList()
public fun emptySet(): Set<Nothing> = Collections.emptySet()
public fun <V> emptyMap(): Map<Nothing, V> = Collections.emptyMap()