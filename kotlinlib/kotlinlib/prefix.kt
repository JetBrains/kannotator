package kotlinlib

public fun String.prefix(length: Int): String = if (length == 0) "" else substring(0, length)

// Absence of default can be easily replaced with "str.prefixUpTo(ch) ?: default"
public fun String.prefixUpToLast(ch: Char): String? {
    val lastIndex = lastIndexOf(ch)
    return if (lastIndex != -1) prefix(lastIndex) else null
}

public fun String.prefixUpToLast(s: String): String? {
    val lastIndex = lastIndexOf(s)
    return if (lastIndex != -1) prefix(lastIndex) else null
}

public fun String.prefixUpTo(ch: Char): String? {
    val firstIndex = indexOf(ch)
    return if (firstIndex != -1) prefix(firstIndex) else null
}

public fun String.prefixUpTo(s: String): String? {
    val firstIndex = indexOf(s)
    return if (firstIndex != -1) prefix(firstIndex) else null
}