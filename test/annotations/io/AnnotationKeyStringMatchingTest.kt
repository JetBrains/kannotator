package annotations.io

import java.io.File
import java.util.LinkedHashSet
import org.junit.Assert.*
import org.junit.Test
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.asm.util.forEachField
import org.jetbrains.kannotator.asm.util.forEachMethod
import org.jetbrains.kannotator.declarations.*
import kotlinlib.*
import util.*

/**
 * Tests that each annotations key (from `annotations.xml` in `lib` directory)
 * is present in some jar (in lib directory).
 * That is, we test we are able to parse existing third-party annotations.
 **/
class AnnotationKeyStringMatchingTest {

    fun doTest(jarsDir: File, annotationsDir: File) {
        // collecting annotation keys from annotations.xml
        val allKeyStringsFromAnnotationFiles = LinkedHashSet<String>()
        annotationsDir.collectAllAnnotationKeysTo(allKeyStringsFromAnnotationFiles)

        println("collected ${allKeyStringsFromAnnotationFiles.size} annotation keys")

        // removing annotation keys found in jars
        recurseIntoJars(jarsDir) {
            file, owner, reader ->
            reader.forEachMethod {
                owner, access, name, desc, signature ->
                val method = Method(ClassName.fromInternalName(owner), access, name, desc, signature)
                PositionsForMethod(method).forEachValidPosition { annotationPos ->
                    allKeyStringsFromAnnotationFiles.remove(annotationPos.toAnnotationKey())
                }
            }
            reader.forEachField {
                owner, access, name, desc, signature, value ->
                val field = Field(ClassName.fromInternalName(owner), access, name, desc, signature, value)
                val annotationPosition = getFieldTypePosition(field)
                allKeyStringsFromAnnotationFiles.remove(annotationPosition.toAnnotationKey())
            }
        }

        assertTrue(
                "Unmatched annotations keys:\n" + allKeyStringsFromAnnotationFiles.sorted().joinToString("\n"),
                allKeyStringsFromAnnotationFiles.isEmpty()
        )
    }

    @Test
    fun testLibFolder() {
        doTest(File("lib"), File("lib"))
    }
}
