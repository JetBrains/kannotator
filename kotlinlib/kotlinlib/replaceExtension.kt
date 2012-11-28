package kotlinlib

import java.io.File

public fun File.replaceExtension(newExtension: String): File {
    val name = getName()
    val index = name.lastIndexOf('.')
    val noExtension = if (index < 0) name else name.substring(0, index)
    return File(getParent(), noExtension + "." + newExtension)
}