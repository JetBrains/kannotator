package kotlinlib

public fun Iterator<String?>.join(separator: String = ""): String {
    return buildString {
        while (hasNext()) {
            it.append(next())
            if (hasNext()) {
                it.append(separator)
            }
        }
    }
}

public fun Iterable<String?>.join(separator: String = ""): String = iterator().join(separator)

public fun Array<out String?>.join(separator: String = ""): String {
    return buildString {
        for (i in this.indices) {
            it.append(this[i])
            if (i < size - 1) {
                it.append(separator)
            }
        }
    }
}

