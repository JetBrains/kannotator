package annotations.io

import java.io.File
import java.io.FileWriter
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InputStream
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.main.ProgressMonitor
import util.INFERRERS
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import junit.framework.Assert
import java.util.LinkedHashSet
import org.jetbrains.kannotator.annotations.io.writeAnnotations
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import java.util.Collections
import kotlinlib.*
import org.jetbrains.kannotator.annotationsInference.nullability.*
import java.util.LinkedHashMap
import org.jetbrains.kannotator.declarations.*
import java.util.HashMap

public class JsrWriterTest : AbstractWriteAnnotationTest() {

    Test fun testReadingAnnotationsBackAfterInsertAnnotationsUtility() {
        val jarFile = File("lib/asm-debug-all-4.0.jar")
        val dataDir = File("jsrWriter/testData/asm/data")
        val originalAnnotationsFile = File(dataDir, "annotations.xml")
        val typePositionAndAnnotationData = readAnnotationsForAllPositionsInJarFile(jarFile, originalAnnotationsFile)

        val tempFilesDir = File("jsrWriter/testData/asm/temp")
        tempFilesDir.mkdirs()
        val annotationsInJsrFormatFile = File(tempFilesDir, "asm.jaif")

        println("Saved file: ${annotationsInJsrFormatFile.getAbsolutePath()}")
        writeAnnotationsJSRStyle(FileWriter(annotationsInJsrFormatFile), typePositionAndAnnotationData)

        val originalClassFile = File(dataDir, "ClassVisitor.class")
        val classFileCopy = File(tempFilesDir, originalClassFile.name)
        originalClassFile.copyTo(classFileCopy)

        insertAnnotationInClassFile(classFileCopy.getAbsolutePath(), annotationsInJsrFormatFile.getAbsolutePath())

        val inferenceResult = inferAnnotations(FileBasedClassSource(arrayListOf(classFileCopy)),
                Collections.emptyList(), INFERRERS, ProgressMonitor(), true, true,
                Collections.emptyMap(), Collections.emptyMap(), { true }, Collections.emptyMap()
        )

        val nullability = inferenceResult.groupByKey[NULLABILITY_KEY]!!.inferredAnnotations as Annotations<NullabilityAnnotation>
        val members = LinkedHashSet<ClassMember>()
        nullability.forEach {(pos, annotation) -> if (!members.contains(pos.member)) members.add(pos.member) }

        val methodsToAnnotationsMap = methodsToAnnotationsMapForNullabilityAnnotations(members, nullability)

        val annotationsReadFromModifiedClassFile = File(tempFilesDir, "annotations.xml")
        writeAnnotations(FileWriter(annotationsReadFromModifiedClassFile), methodsToAnnotationsMap)
        val result = annotationsReadFromModifiedClassFile.readText().trim().toUnixSeparators()
        val expected = originalAnnotationsFile.readText().trim().toUnixSeparators()
        Assert.assertEquals(expected, result)
    }

    // annotations-file-utilities are called from another process, because they use some hacked version of javac and asm
    // also they are modifying boot classpath to launch
    private fun insertAnnotationInClassFile(classFile: String, jsrAnnotationFile: String) {
        val INSERT_ANNOTATION_TO_BYTECODE_MAIN_CLASS = "annotations.io.classfile.ClassFileWriter"
        val PATH_TO_ANNOTATION_FILE_UTILITIES_JAR = "jsrWriter/lib/annotation-file-utilities.jar"
        val PATH_TO_HACKED_JAVAC_JAR = "jsrWriter/lib/javac.jar"
        val classPathEntry = "$PATH_TO_ANNOTATION_FILE_UTILITIES_JAR${File.pathSeparator}$PATH_TO_HACKED_JAVAC_JAR"
        val cmd = "java -ea -Xbootclasspath/p:$classPathEntry -classpath $classPathEntry " +
        "$INSERT_ANNOTATION_TO_BYTECODE_MAIN_CLASS $classFile $jsrAnnotationFile"
        println("Executing $cmd")
        val rt = Runtime.getRuntime()
        val process = rt.exec(cmd)
        printProcessOutput(process)
        val exitVal = process.waitFor()
        println("Process exitValue: " + exitVal);
    }

    private fun printProcessOutput(process: Process) {
        val stderr = streamToString(process.getErrorStream()!!)
        if (!stderr.isEmpty()) {
            Assert.fail("An error while executing process:\n$stderr")
        }
        println(streamToString(process.getInputStream()!!))
    }

    private fun streamToString(stream: InputStream): String {
        val br = BufferedReader(InputStreamReader(stream))
        var line: String?
        val sb = StringBuilder()
        do {
            line = br.readLine()
            if (line != null) {
                sb.append(line)
            }
        } while (line != null)
        return sb.toString()
    }

    fun methodsToAnnotationsMapForNullabilityAnnotations(
            members: Collection<ClassMember>,
            nullability: Annotations<NullabilityAnnotation>
    ): Map<AnnotationPosition, MutableList<AnnotationData>> {
        val annotations = LinkedHashMap<AnnotationPosition, MutableList<AnnotationData>>()

        fun processPosition(pos: AnnotationPosition) {
            val nullAnnotation = nullability[pos]
            val data: AnnotationData
            if (nullAnnotation == NullabilityAnnotation.NOT_NULL) {
                data = AnnotationDataImpl(JB_NOT_NULL, HashMap())
                annotations[pos] = arrayListOf<AnnotationData>(data)
            } else if (nullAnnotation == NullabilityAnnotation.NULLABLE) {
                data = AnnotationDataImpl(JB_NULLABLE, HashMap())
                annotations[pos] = arrayListOf<AnnotationData>(data)
            }
        }

        for (m in members) {
            if (m is Method) {
                PositionsForMethod(m).forEachValidPosition { pos -> processPosition(pos) }
            } else if (m is Field) {
                processPosition(getFieldTypePosition(m))
            }
        }
        return annotations
    }

}
