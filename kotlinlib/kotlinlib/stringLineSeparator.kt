package kotlinlib

public fun String.toSystemLineSeparators(): String {
    return this.convertLineSeparators(LINE_SEPARATOR)
}

public fun String.toUnixSeparators(): String {
    return this.convertLineSeparators("\n")
}

public fun String.toWindowsSeparators(): String {
    return this.convertLineSeparators("\r\n")
}

public fun String.convertLineSeparators(newSeparator: String): String {
    return buildString {
        sb ->
            var i = 0
            while (i < this.size) {
                val c = this[i]
                when {
                    c == '\n' ->  sb.append(newSeparator)
                    c == '\r' &&
                    this.getOrElse(i + 1, null) == '\n' -> {
                        sb.append(newSeparator)
                        i++
                    }
                    else -> sb.append(c);
                }
                i++
            }
    }
}
