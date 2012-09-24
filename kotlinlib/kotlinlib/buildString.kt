package kotlinlib

public fun buildString(body: (sb: StringBuilder) -> Unit): String {
    val sb = StringBuilder()
    body(sb)
    return sb.toString()
}