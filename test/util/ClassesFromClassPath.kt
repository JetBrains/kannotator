package util

import org.jetbrains.kannotator.index.ClassSource
import org.objectweb.asm.ClassReader

class ClassesFromClassPath(vararg val canonicalNames: String) : ClassSource {
    override fun forEach(body: (ClassReader) -> Unit) {
        for (canonical in canonicalNames) {
            body(ClassReader(canonical))
        }
    }
}