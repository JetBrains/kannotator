package kotlinlib

import java.util.Collections
import java.util.Arrays

// Already in the library
public fun <T> Iterable<T>?.orEmpty(): Iterable<T> = if (this == null) Collections.emptyList() else this
public fun <T> Iterator<T>?.orEmpty(): Iterator<T> = if (this == null) Collections.emptyList<T>().iterator() else this

private val EMPTY_OBJECT_ARRAY = Array<Any?>(0, {null})
private val EMPTY_INT_ARRAY = IntArray(0)
private val EMPTY_CHAR_ARRAY = CharArray(0)
private val EMPTY_BYTE_ARRAY = ByteArray(0)
private val EMPTY_SHORT_ARRAY = ShortArray(0)
private val EMPTY_LONG_ARRAY = LongArray(0)
private val EMPTY_FLOAT_ARRAY = FloatArray(0)
private val EMPTY_DOUBLE_ARRAY = DoubleArray(0)
private val EMPTY_BOOLEAN_ARRAY = BooleanArray(0)

// This one is there in the Arrays.kt, but the signature is wrong, so thus the workaround
public fun <T> Array<out T>?.orEmptyArray(): Array<out T> = if (this == null) EMPTY_OBJECT_ARRAY as Array<out T> else this
public fun <T> IntArray?.orEmpty(): IntArray = if (this == null) EMPTY_INT_ARRAY else this
public fun <T> CharArray?.orEmpty(): CharArray = if (this == null) EMPTY_CHAR_ARRAY else this
public fun <T> ByteArray?.orEmpty(): ByteArray = if (this == null) EMPTY_BYTE_ARRAY else this
public fun <T> ShortArray?.orEmpty(): ShortArray = if (this == null) EMPTY_SHORT_ARRAY else this
public fun <T> LongArray?.orEmpty(): LongArray = if (this == null) EMPTY_LONG_ARRAY else this
public fun <T> FloatArray?.orEmpty(): FloatArray = if (this == null) EMPTY_FLOAT_ARRAY else this
public fun <T> DoubleArray?.orEmpty(): DoubleArray = if (this == null) EMPTY_DOUBLE_ARRAY else this
public fun <T> BooleanArray?.orEmpty(): BooleanArray = if (this == null) EMPTY_BOOLEAN_ARRAY else this