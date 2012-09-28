package kotlinlib

fun String.suffixAfterLast(delimiter: String): String {
    val index = this.lastIndexOf(delimiter)
    if (index < 0) return this
    return this.substring(index + 1)
}

fun String.suffixAfterLast(delimiter: Char): String {
    val index = this.lastIndexOf(delimiter)
    if (index < 0) return this
    return this.substring(index + 1)
}

fun String.suffixAfter(delimiter: String): String {
    val index = this.lastIndexOf(delimiter)
    if (index < 0) return this
    return this.substring(index + 1)
}

fun String.suffixAfter(delimiter: Char): String {
    val index = this.lastIndexOf(delimiter)
    if (index < 0) return this
    return this.substring(index + 1)
}