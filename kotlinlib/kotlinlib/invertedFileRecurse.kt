package kotlinlib

import java.io.File

public fun File.invertedRecurse(block: (File) -> Unit): Unit {
    if (isDirectory()) {
        for (child in listFiles()!!) {
            child.invertedRecurse(block)
        }
    }
    block(this)
}
