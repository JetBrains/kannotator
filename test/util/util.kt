package util

import java.io.File
import org.jetbrains.kannotator.declarations.ClassName
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.jetbrains.kannotator.util.processJar
import java.util.ArrayList
import kotlinlib.recurseFiltered

fun recurseIntoJars(libDir: File, block: (jarFile: File, classType: Type, classReader: ClassReader) -> Unit) {
    libDir.recurse {
        file ->
        if (file.isFile() && file.getName().endsWith(".jar")) {
            println("Processing: $file")

            processJar(file, block)
        }
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

fun findJarFiles(dirs: Collection<File>): Collection<File> {
    val jars = ArrayList<File>()
    for (dir in dirs) {
        dir.recurseFiltered({it.extension == "jar"}) {
            jars.add(it)
        }
    }
    return jars
}