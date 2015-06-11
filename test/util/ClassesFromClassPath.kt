package util

import org.jetbrains.kannotator.index.ClassSource
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.declarations.*
import java.io.IOException
import java.io.InputStream

fun ClassesFromClassPath(vararg classNames: String): ClassSource = ClassesFromClassPath(classNames.toList())
fun ClassesFromClassPath(classNames: Collection<ClassName>): ClassSource
        = ClassesFromClassPath(classNames.map {it.internal})

class ClassesFromClassPath(val classNames: Collection<String>) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (name in classNames) {
            body(getClassReader(name))
        }
    }
}

fun Classes(vararg classes: Class<*>): ClassSource = Classes(classes.toList())

class Classes(val classes: Collection<Class<*>>) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (javaClass in classes) {
            body(getClassReader(javaClass))
        }
    }
}

fun getClassReader(klass: Class<*>): ClassReader {
    return getClassReader(klass.getName())
}

fun getClassReader(className: String): ClassReader {
    val resourceAsStream = getClassAsStream(className)
    if (resourceAsStream == null) {
        throw Exception("Couldn't find resource $className")
    }
    val classReader = ClassReader(resourceAsStream)
    resourceAsStream.close();
    return classReader
}

fun getClassAsStream(className: String): InputStream? {
    val appClassLoader = javaClass<ClassesFromClassPath>().getClassLoader()
    if (appClassLoader == null) {
        return null
    }
    val resourceName = className.replace("\\.".toRegex(), "/") + ".class"
    return appClassLoader.getResourceAsStream(resourceName)
}
