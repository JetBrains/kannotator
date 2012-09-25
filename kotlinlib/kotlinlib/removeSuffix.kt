package kotlinlib

public fun String.removeSuffix(suffix: String): String {
    if (!endsWith(suffix)) return this
    return substring(0, size - suffix.size)
}