package org.jetbrains.kannotator.util

import java.io.File
import org.objectweb.asm.Type
import org.objectweb.asm.ClassReader
import java.util.jar.JarFile
import java.util.Enumeration
import java.util.jar.JarEntry

fun processJar(file: File, block: (jarFile: File, classType: Type, classReader: ClassReader) -> Unit) {
    val jar = JarFile(file)
    for (entry in jar.entries() as Enumeration<JarEntry>) { //todo KT-2872
        val name = entry.getName()
        if (!name.endsWith(".class")) continue

        val internalName = name.removeSuffix(".class")
        val classType = Type.getType("L$internalName;")

        val inputStream = jar.getInputStream(entry)
        val classReader = ClassReader(inputStream)

        block(file, classType, classReader)
    }
}

