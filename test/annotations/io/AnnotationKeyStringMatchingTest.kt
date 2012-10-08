package annotations.io

import java.io.File
import java.util.LinkedHashSet
import junit.framework.TestCase
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.PositionsWithinMember
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import java.io.FileReader
import kotlinlib.*
import java.util.regex.Pattern
import junit.framework.Assert.*
import org.jetbrains.kannotator.util.processJar
import org.jetbrains.kannotator.asm.util.forEachMethod

class AnnotationKeyStringMatchingTest : TestCase() {

    fun doTest(jarDirsOrFiles: Collection<File>, annotationDirs: Collection<File>) {
        val allKeyStringsFromAnnotations = LinkedHashSet<String>()
        for (annotationDir in annotationDirs) {
            addFromAnnotationDir(annotationDir, allKeyStringsFromAnnotations)
        }

        for (dirOrFile in jarDirsOrFiles) {
            dirOrFile.recurseFiltered({it.extension == "jar"}) {
                file ->
                visitAllInJar(file, {
                    allKeyStringsFromAnnotations.remove(it)
                })
            }
        }

        val methodKeysFromAnnotationFiles = allKeyStringsFromAnnotations
                .iterator()
                .filter { it.contains("(") && "@" !in it }
                .toSet()

        assertTrue("Unmatched annotations keys:\n" + methodKeysFromAnnotationFiles.toSortedList().join("\n"),
                   methodKeysFromAnnotationFiles.isEmpty())
    }

    fun testLib() {
        doTest(arrayList(File("lib")),
                arrayList(File("lib")))
    }

    fun testJdk() {
        doTest(arrayList(File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/classes.jar")),
                arrayList(File("/Volumes/WD600/work/kotlin/jdk-annotations")))
    }

}

fun visitAllInJar(jarFile: File, handler: (String) -> Unit) {
    var count = 0
    processJar(jarFile) {
        file, owner, reader ->
        print("*")
        count++
        if (count % 130 == 0) println()
        reader.forEachMethod {
            owner, access, name, desc, signature ->
            val method = Method(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature)
            PositionsWithinMember(method).forEachValidPosition {
                handler(it.toAnnotationKey())
            }
        }
    }
    println()
}

fun addFromAnnotationDir(annotationDir: File, allKeyStrings: MutableSet<String>) {
    annotationDir recurse {
        file ->
        if (file.isFile() && file.getName() == "annotations.xml") {
            addFromAnnotationFile(file, allKeyStrings)
        }
    }
}

fun addFromAnnotationFile(annotationFile: File, allKeyStrings: MutableSet<String>) {
    FileReader(annotationFile) use {
        parseAnnotations(it,
                {
                    annotationKey, data ->
                    allKeyStrings.add(annotationKey)
                },
                {
                    throw IllegalArgumentException(it)
                })
    }
}