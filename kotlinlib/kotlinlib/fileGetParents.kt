package kotlinlib

import java.io.File
import java.util.ArrayList

public fun File.getParents(): List<String> {
    val result = ArrayList<String>()
    var current = this.parentFile
    while (current != null) {
        result.add(current.name)
        current = current.parentFile
    }
    return result.reversed()
}