package kotlinlib

import java.io.File

public fun File.deleteRecursively(): Unit {
    invertedRecurse { file -> file.delete() }
}
