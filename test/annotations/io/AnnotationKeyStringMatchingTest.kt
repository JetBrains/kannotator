package annotations.io

import java.io.File
import java.io.FileReader
import java.util.LinkedHashSet
import org.junit.Assert.*
import org.junit.Test
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.asm.util.forEachMethod
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.util.processJar
import org.jetbrains.kannotator.simpleErrorHandler
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlinlib.*
import java.util.regex.Pattern
import org.jetbrains.kannotator.asm.util.forEachField

/**
 * tests that each annotations key (from `annotations.xml` in `lib` directory)
 * is present in some jar (in lib directory)
 **/
class AnnotationKeyStringMatchingTest {

    fun doTest(jarDirsOrFiles: Collection<File>, annotationDirs: Collection<File>) {
        val allKeyStringsFromAnnotationFiles = LinkedHashSet<String>()
        for (annotationDir in annotationDirs) {
            annotationDir.collectAllAnnotationKeysTo(allKeyStringsFromAnnotationFiles)
        }

        println("collected ${allKeyStringsFromAnnotationFiles.size()} annotation keys")

        for (dirOrFile in jarDirsOrFiles) {
            dirOrFile.recurseFiltered({it.extension == "jar"}) {
                file ->
                visitAllAnnotationKeysInJar(file, { annotationKey ->
                    allKeyStringsFromAnnotationFiles.remove(annotationKey)
                })
            }
        }

        assertTrue(
                "Unmatched annotations keys:\n" + allKeyStringsFromAnnotationFiles.toSortedList().join("\n"),
                allKeyStringsFromAnnotationFiles.isEmpty()
        )
    }

    Test
    fun testLibFolder() {
        doTest(arrayListOf(File("lib")), arrayListOf(File("lib")))
    }
}

/** call handler for each annotation field/method annotation */
private fun visitAllAnnotationKeysInJar(jarFile: File, annotationKeyHandler: (String) -> Unit) {
    processJar(jarFile) {
        file, owner, reader ->
        reader.forEachMethod {
            owner, access, name, desc, signature ->
            val method = Method(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature)
            PositionsForMethod(method).forEachValidPosition {
                annotationKeyHandler(it.toAnnotationKey())
            }
        }
        reader.forEachField {
            owner, access, name, desc, signature, value ->
            val field = Field(ClassName.fromInternalName(reader.getClassName()), access, name, desc, signature, value)
            val fieldTypePosition = getFieldTypePosition(field)
            annotationKeyHandler(fieldTypePosition.toAnnotationKey())
        }
    }
}

/** collects all annotations keys found in annotations.xml in this file/dirs */
fun File.collectAllAnnotationKeysTo(allKeyStrings: MutableSet<String>) {
    recurseFiltered(
            { it.isFile() && it.name == "annotations.xml"},
            { it.loadAnnotationKeysTo(allKeyStrings)}
    )
}
/** assuming that file is annotations.xml, load annotations keys */
private fun File.loadAnnotationKeysTo(allKeyStrings: MutableSet<String>) {
    FileReader(this) use {
        reader ->
        parseAnnotations(reader,
                {
                    annotationKey, data ->
                    allKeyStrings.add(annotationKey)
                },
                simpleErrorHandler {
                    kind, message ->
                    throw IllegalArgumentException("$kind, $message")
                })
    }
}