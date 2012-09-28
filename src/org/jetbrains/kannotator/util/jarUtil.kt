package org.jetbrains.kannotator.util

import java.io.File
import org.objectweb.asm.Type
import org.objectweb.asm.ClassReader
import java.util.jar.JarFile
import kotlinlib.removeSuffix

fun processJar(file: File, block: (jarFile: File, classType: Type, classReader: ClassReader) -> Unit) {
    val jar = JarFile(file)
    for (entry in jar.entries()) {
        val name = entry!!.getName()!!
        if (!name.endsWith(".class")) continue

        val internalName = name.removeSuffix(".class")
        val classType = Type.getType("L$internalName;")

        val inputStream = jar.getInputStream(entry)
        val classReader = ClassReader(inputStream)

        block(file, classType, classReader)
    }
}

