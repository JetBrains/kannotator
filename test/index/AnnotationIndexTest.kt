package index

import junit.framework.TestCase
import junit.framework.Assert.*
import annotations.io.addFromAnnotationDir
import java.io.File
import java.util.HashSet
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import util.findJarFiles
import org.jetbrains.kannotator.annotations.io.toAnnotationKey

class AnnotationIndexTest : TestCase() {
    fun test() {
        val keys = HashSet<String>()
        val dir = File("lib")
        addFromAnnotationDir(dir, keys)

        val jars = findJarFiles(arrayList(dir))

        val source = FileBasedClassSource(jars)
        val index = DeclarationIndexImpl(source)

        for (key in keys) {
            if ("(" !in key) continue // no fields
            val position = index.findPositionByAnnotationKeyString(key)
            if (position == null) {
                fail("Position not forund for $key")
            }
            else {
                assertEquals(key, position.toAnnotationKey())
            }
        }
    }
}