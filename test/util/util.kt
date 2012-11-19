package util

import java.io.File
import org.jetbrains.kannotator.declarations.ClassName
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.jetbrains.kannotator.util.processJar
import java.util.ArrayList
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.index.ClassSource
import junit.framework.Assert

fun recurseIntoJars(libDir: File, block: (jarFile: File, classType: Type, classReader: ClassReader) -> Unit) {
    libDir.recurse {
        file ->
        if (file.isFile() && file.getName().endsWith(".jar")) {
            println("Processing: $file")

            processJar(file, block)
        }
    }
}

fun getAllClassesWithPrefix(prefix: String): ClassSource {
    val result = arrayList<ClassName>()

    findJarsInLibFolder().forEach {
        recurseIntoJars(it) {
            f, classType, classReader ->
            val name = ClassName.fromType(classType)
            if (name.internal.startsWith(prefix)) {
                result.add(name)
            }
        }
    }

    return ClassesFromClassPath(result)
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

fun findJarsInLibFolder(): List<File> {
    val jars = ArrayList<File>()
    File("lib").recurse {
        file ->
        if (file.isFile() && file.getName().endsWith(".jar")) {
            jars.add(file)
        }
    }
    return jars
}


fun assertEqualsOrCreate(expectedFile: File, actual: String, failOnNoData: Boolean = true): Boolean {
    if (!expectedFile.exists()) {
        expectedFile.getParentFile()!!.mkdirs()
        expectedFile.writeText(actual)
        if (failOnNoData) {
            Assert.fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
        }
        return false
    }

    val expected = expectedFile.readText()

    Assert.assertEquals(expected, actual)
    return true
}