package kotlinlib

val SYSTEM_LINE_SEPARATOR: String = System.getProperty("line.separator")!!

public fun String.toSystemLineSeparators(): String = this.convertLineSeparators(SYSTEM_LINE_SEPARATOR)
public fun String.toUnixSeparators(): String = convertLineSeparators("\n")
public fun String.toWindowsSeparators(): String = convertLineSeparators("\r\n")

public fun String.convertLineSeparators(newSeparator: String): String {
    return StringBuilder {
        var i = 0
        while (i < this.size) {
            val c = this[i]
            when {
                c == '\n' -> append(newSeparator)
                c == '\r' && getOrElse(i + 1, null: Char?) == '\n' -> {
                    // KT-209
                    append(newSeparator)
                    i++
                }
                else -> append(c);
            }
            i++
        }
    }.toString()
}
