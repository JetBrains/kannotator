package util

import org.jetbrains.kannotator.index.ClassSource
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.declarations.*

fun ClassesFromClassPath(vararg classNames: String): ClassSource = ClassesFromClassPath(classNames.toList())
fun ClassesFromClassPath(classNames: Collection<ClassName>): ClassSource
        = ClassesFromClassPath(classNames.map {it.internal})

class ClassesFromClassPath(val classNames: Collection<String>) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (name in classNames) {
            body(ClassReader(name))
        }
    }
}