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
        val jarDirs = arrayList(
//                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes"),
//                java.io.File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib"),
                File("lib")
        )

        val annotationDirs = arrayList(
//                java.io.File("/Users/abreslav/work/kotlin/jdk-annotations"),
                File("lib")
        )

        val keys = HashSet<String>()
        for (dir in annotationDirs) {
            addFromAnnotationDir(dir, keys)
        }

        val jars = findJarFiles(jarDirs)

        val source = FileBasedClassSource(jars)
        val index = DeclarationIndexImpl(source, failOnDuplicates = false)

        for (key in keys) {
            if ("(" !in key) continue // no fields
            if ("@" in key) continue // bug in IDEA
            val position = index.findPositionByAnnotationKeyString(key)
            if (position == null) {
                fail("Position not found for $key")
            }
        }

    }
}