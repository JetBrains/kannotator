package kotlinlib

public fun String.getOrElse<R: Char?>(index: Int, default: R): R
        = if (index in 0..length() - 1) this[index] as R else default

public fun <T, R: T?> Array<T>.getOrElse(index: Int, default: R): R
        = if (index in 0..size() - 1) this[index] as R else default

public fun <T, R: T?> List<T>.getOrElse(index: Int, default: R): R
        = if (index in 0..size() - 1) this[index] as R else default