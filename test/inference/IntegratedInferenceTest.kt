package inference

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import java.util.Collections
import java.io.PrintStream
import java.io.FileOutputStream
import kotlinlib.println
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.declarations.Method
import kotlinlib.removeSuffix
import java.io.FileInputStream
import interpreter.readWithBuffer
import java.util.TreeMap
import org.jetbrains.kannotator.annotationsInference.Annotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.util.ArrayList
import util.assertEqualsOrCreate
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.AnnotationPosition
import kotlin.test.assertTrue
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.main.*
import util.findJarsInLibFolder
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.junit.Assert

class IntegratedInferenceTest : TestCase() {
    private fun <A: Any> reportConflicts(
            testName: String,
            conflictFile: File,
            inferredAnnotations: Annotations<A>,
            inferrer: AnnotationInferrer<A>
    ) {
        val conflictExceptions = loadConflictExceptions(File("testData/inferenceData/integrated/$testName/exceptions.txt"))
        val conflicts = findAnnotationInferenceConflicts(inferredAnnotations, inferrer, conflictExceptions)
        if (!conflicts.isEmpty()) {
            PrintStream(FileOutputStream(conflictFile)) use {
                p ->
                for ((key, expectedAnn, inferredAnn) in conflicts) {
                    p.println("Conflict at ${key.toAnnotationKey()}")
                    p.println("\t expected: $expectedAnn, inferred: $inferredAnn")
                }
            }
            assertTrue("Found annotation conflicts", false);
        }
    }

    private val INFERRERS = hashMap(
            Pair("nullability", NullabilityInferrer() as AnnotationInferrer<Any>),
            Pair("mutability", MUTABILITY_INFERRER as AnnotationInferrer<Any>)
    )

    private fun doInferenceTest(testedJarSubstring: String) {
        val progressMonitor = object : ProgressMonitor() {
            var currentMethod: Method? = null

            override fun totalFields(fieldCount: Int) {
                println("Total fields: $fieldCount")
            }

            override fun totalMethods(methodCount: Int) {
                println("Total methods: $methodCount")
            }

            override fun processingStepStarted(method: Method) {
                println(method)
                currentMethod = method
            }
        }

        val jars = findJarsInLibFolder().filter { f -> f.getName().contains(testedJarSubstring) }
        Assert.assertEquals("Test failed to find exactly one jar file with request '$testedJarSubstring'", jars.size, 1);

        val annotationFiles = ArrayList<File>()
        File("lib").recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })

        val jar = jars.first()
        println("start: $jar")

        val annotationsMap = try {
            inferAnnotations(FileBasedClassSource(arrayList(jar)), annotationFiles, INFERRERS, progressMonitor, false)
        }
        catch (e: Throwable) {
            throw IllegalStateException("Failed while working on ${progressMonitor.currentMethod}", e)
        }

        for ((testName, annotations) in annotationsMap) {
            val expectedFile = File("testData/inferenceData/integrated/$testName/${jar.getName()}.annotations.txt")
            val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
            outFile.getParentFile()!!.mkdirs()

            reportConflicts(testName, File(expectedFile.getPath().removeSuffix(".txt") + ".conflicts.txt"), annotations, INFERRERS[testName]!!)

            val map = TreeMap<String, Any>()
            annotations forEach {
                pos, ann -> map.put(pos.toAnnotationKey(), ann)
            }

            PrintStream(FileOutputStream(outFile)) use {
                p ->
                for ((key, ann) in map) {
                    p.println(key)
                    p.println(ann)
                }
            }

            assertEqualsOrCreate(expectedFile, outFile.readText(), false)

            outFile.delete()
        }
    }

    fun testAsmDebugAll() = doInferenceTest("asm-debug-all-4.0.jar")
    fun testCollectionsGeneric() = doInferenceTest("collections-generic-4.01.jar")
    fun testColt() = doInferenceTest("colt-1.2.0.jar")
    fun testConcurrent() = doInferenceTest("concurrent-1.3.4.jar")
    fun testGsCollections() = doInferenceTest("gs-collections-2.0.0.jar")
    fun testGsCollectionsApi() = doInferenceTest("gs-collections-api-2.0.0.jar")
    fun testGuava() = doInferenceTest("guava-13.0.1.jar")
    fun testJ3DCore() = doInferenceTest("j3d-core-1.3.1.jar")
    fun testJung3D() = doInferenceTest("jung-3d-2.0.1.jar")
    fun testJung3dDemos() = doInferenceTest("jung-3d-demos-2.0.1.jar")
    fun testJungAlgorithms() = doInferenceTest("jung-algorithms-2.0.1.jar")
    fun testJungApi() = doInferenceTest("jung-api-2.0.1.jar")
    fun testJungGraphImpl() = doInferenceTest("jung-graph-impl-2.0.1.jar")
    fun testJungIo() = doInferenceTest("jung-io-2.0.1.jar")
    fun testJungJai() = doInferenceTest("jung-jai-2.0.1.jar")
    fun testJungJaiSamples() = doInferenceTest("jung-jai-samples-2.0.1.jar")
    fun testJungSamples() = doInferenceTest("jung-samples-2.0.1.jar")
    fun testJungVisualization() = doInferenceTest("jung-visualization-2.0.1.jar")
    fun testJUnit() = doInferenceTest("junit-4.10.jar")
    fun testStaxApi() = doInferenceTest("stax-api-1.0.1.jar")
    fun testVecmath() = doInferenceTest("vecmath-1.3.1.jar")
    fun testWstxAsl() = doInferenceTest("wstx-asl-3.2.6.jar")
    fun testJDK1_7_0_09_rt_jar() = doInferenceTest("jdk_1_7_0_09_rt")
}