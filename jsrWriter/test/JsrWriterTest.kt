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
import org.jetbrains.kannotator.main.InferenceResult
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.*
import org.junit.ComparisonFailure
import org.jetbrains.kannotator.annotations.io.readAnnotationsForAllPositionsInJarFile
import annotations.io.runner.main

public class JsrWriterTest() {

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

        val annotationsReadFromModifiedClassFile = File(tempFilesDir, "annotations.xml")
        writeExistingAnnotations(classFileCopy, annotationsReadFromModifiedClassFile, NULLABILITY_KEY)

        val result = annotationsReadFromModifiedClassFile.readText().trim().toUnixSeparators()
        val expected = originalAnnotationsFile.readText().trim().toUnixSeparators()
        assertSameLinesOrderAgnostic(expected, result)
        tempFilesDir.deleteRecursively()
    }

    Test fun testSimple() {
        val testDir = "jsrWriter/testData/simple"
        val tempFilesDir = File("$testDir/temp")
        val jarPath = "$tempFilesDir/result.jar"
        val jarFile = File(jarPath)
        tempFilesDir.mkdirs()
        val tempFilesDirPath = tempFilesDir.getPath()
        val outClassesDir = "$tempFilesDir/class"
        File(outClassesDir).mkdirs()
        compileFiles("$testDir/data/src", outClassesDir, jarPath)
        readExistingAnnotations(jarFile)
        writeExistingAnnotations(jarFile, File("$tempFilesDirPath/nullability_annotations.xml"), NULLABILITY_KEY)
        writeExistingAnnotations(jarFile, File("$tempFilesDirPath/mutability_annotations.xml"), MUTABILITY_KEY)
        val data = readAnnotationsForAllPositionsInJarFile(jarFile, tempFilesDir)
        val outFilePath = "$tempFilesDirPath/out.jaif"
        writeAnnotationsJSRStyleGroupedByPackage(FileWriter(outFilePath), data)
        val result = File(outFilePath).readText().trim().toUnixSeparators()
        val expected = File("$testDir/data/expected.jaif").readText().trim().toUnixSeparators()
        assertSameLinesOrderAgnostic(expected, result)
        tempFilesDir.deleteRecursively()
    }

    Test fun testRunner() {
        val testDir = "jsrWriter/testData/main"
        val dataDir = "$testDir/data"
        val tempFilesDir = File("$testDir/temp")
        val resultingFile = "$tempFilesDir/output.jaif"
        main(array("$dataDir/some.jar", "$dataDir/annotations", resultingFile))
        val result = File(resultingFile).readText().trim().toUnixSeparators()
        val expected = File("$dataDir/expected.jaif").readText().trim().toUnixSeparators()
        Assert.assertEquals(expected, result)
        tempFilesDir.deleteRecursively()
    }


    Test fun testWholeAsm() {
        val tempFilesDir = File("jsrWriter/testData/wholeAsm/temp")
        val resultingFile = "$tempFilesDir/output.jaif"
        main(array("lib/asm-debug-all-4.0.jar", "lib/asm.annotations", resultingFile))
        val result = File(resultingFile).readText().trim().toUnixSeparators()
        val expected = File("jsrWriter/testData/wholeAsm/expected.jaif").readText().trim().toUnixSeparators()
        Assert.assertEquals(expected, result)
        tempFilesDir.deleteRecursively()
    }

    fun assertSameLinesOrderAgnostic(expected: String, actual: String) {
        if (!expected.split("\n").toSet().equals(actual.split("\n").toSet())) {
            throw ComparisonFailure(null, expected, actual)
        }
    }


    fun compileFiles(srcPath: String, dstPath: String, jarPath: String) {
        execCmd("javac -d $dstPath -sourcepath src -cp lib/jetbrains-annotations.jar $srcPath/A.java $srcPath/somep/A.java")
        execCmd("jar -cvf $jarPath $dstPath")
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
        execCmd(cmd)
    }

    private fun execCmd(cmd: String) {
        println("Executing $cmd")
        val rt = Runtime.getRuntime()
        val process = rt.exec(cmd)
        printProcessOutput(process)
        val exitVal = process.waitFor()
        println("Process exitValue: " + exitVal)
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

    fun <K> methodsToAnnotationsMapForMutabilityOrNullabilityAnnotations(
            members: Collection<ClassMember>,
            annotations: Annotations<K>
    ): Map<AnnotationPosition, MutableList<AnnotationData>> {
        val result = LinkedHashMap<AnnotationPosition, MutableList<AnnotationData>>()

        fun processPosition(pos: AnnotationPosition) {
            val annotation: K? = annotations[pos]
            var data: AnnotationData? = null
            if (annotation == NullabilityAnnotation.NOT_NULL) {
                data = AnnotationDataImpl(JB_NOT_NULL, HashMap())
            } else if (annotation == NullabilityAnnotation.NULLABLE) {
                data = AnnotationDataImpl(JB_NULLABLE, HashMap())
            }  else if (annotation == MutabilityAnnotation.MUTABLE) {
                data = AnnotationDataImpl(JB_MUTABLE, HashMap())
            }  else if (annotation == MutabilityAnnotation.READ_ONLY) {
                data = AnnotationDataImpl(JB_READ_ONLY, HashMap())
            }
            if (data != null) {
                result[pos] = arrayListOf<AnnotationData>(data!!)
            }
        }

        for (m in members) {
            if (m is Method) {
                PositionsForMethod(m).forEachValidPosition { pos -> processPosition(pos) }
            } else if (m is Field) {
                processPosition(getFieldTypePosition(m))
            }
        }
        return result
    }

    fun readExistingAnnotations(jarOrClassFile: File): InferenceResult<AnalysisType> {
        return inferAnnotations(FileBasedClassSource(arrayListOf(jarOrClassFile)),
                Collections.emptyList(), INFERRERS, ProgressMonitor(), true, true,
                Collections.emptyMap(), Collections.emptyMap(), { true }, Collections.emptyMap()
        )
    }

    fun <K : AnalysisType> writeExistingAnnotations(classOrJarFile: File, dst: File, key: K) {
        val inferenceResult = readExistingAnnotations(classOrJarFile)

        val annotations = inferenceResult.groupByKey[key]!!.inferredAnnotations
        val members = LinkedHashSet<ClassMember>()
        annotations.forEach {(pos, annotation) -> if (!members.contains(pos.member)) members.add(pos.member) }

        val methodsToAnnotationsMap = methodsToAnnotationsMapForMutabilityOrNullabilityAnnotations(members, annotations)

        writeAnnotations(FileWriter(dst), methodsToAnnotationsMap)
    }

}
