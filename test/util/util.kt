package util

import java.io.File
import java.util.jar.JarFile
import kotlinlib.removeSuffix
import org.jetbrains.kannotator.declarations.ClassName
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type

fun recurseIntoJars(libDir: File, block: (jarFile: File, classType: Type, classReader: ClassReader) -> Unit) {
    libDir.recurse {
        file ->
        if (file.isFile() && file.getName().endsWith(".jar")) {
            println("Processing: $file")

            processJar(file, block)
        }
    }
}

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

fun getAllClassesWithPrefix(prefix: String): List<ClassName> {
    val classPath = System.getProperty("java.class.path")!!
    val result = arrayList<ClassName>()

    for (jar in classPath.split(File.pathSeparatorChar)) {
        recurseIntoJars(File(jar)) {
            f, classType, classReader ->
            val name = ClassName.fromType(classType)
            if (name.internal.startsWith(prefix)) {
                result.add(name)
            }
        }
    }

    return result
}
