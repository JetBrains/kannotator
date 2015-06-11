package kotlinlib

import java.io.File
import java.util.ArrayList

public fun File.getParents(): List<String> {
    val result = ArrayList<String>()
    var current = this.getParentFile()
    while (current != null) {
        result.add(current.getName())
        current = current.getParentFile()
    }
    return result.reverse()
}