package kotlinlib

import java.io.File

public fun File.recurseFiltered(fileFilter: (File) -> Boolean = {true}, block: (File) -> Unit): Unit {
    if (fileFilter(this)) {
        block(this)
    }
    if (this.isDirectory) {
        for (child in this.listFiles()!!) {
            child.recurseFiltered(fileFilter, block)
        }
    }
}

