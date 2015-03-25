package kotlinlib

public fun String.replaceSuffix(oldSuffix: String, newSuffix: String): String {
    return removeSuffix(oldSuffix) + newSuffix
}
