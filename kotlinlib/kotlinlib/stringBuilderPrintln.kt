package kotlinlib

val SYSTEM_LINE_SEPARATOR: String = System.getProperty("line.separator")!!

public fun StringBuilder.println(): StringBuilder {
    return append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Any?): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Byte): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Short): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Int): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Long): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Float): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Double): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Char): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

public fun StringBuilder.println(a: Boolean): StringBuilder {
    return append(a).append(SYSTEM_LINE_SEPARATOR)
}

