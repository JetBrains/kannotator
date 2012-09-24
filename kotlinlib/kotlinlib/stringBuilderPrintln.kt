package kotlinlib

val LINE_SEPARATOR: String = System.getProperty("line.separator")!!

public inline fun StringBuilder.println(a: Any?): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Byte): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Short): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Int): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Long): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Float): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Double): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Char): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

public inline fun StringBuilder.println(a: Boolean): StringBuilder {
    return append(a).append(LINE_SEPARATOR)
}

