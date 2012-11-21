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
    private fun checkConflicts(conflictFile: File, inferred: Annotations<Any>) {
        val existingAnnotations = (inferred as AnnotationsImpl<Any>).delegate
        if (existingAnnotations != null) {
            val conflicts = ArrayList<Triple<AnnotationPosition, Any, Any>>()
            existingAnnotations forEach {
                pos, ann ->
                if (inferred[pos] != ann) {
                    conflicts.add(Triple(pos, ann, inferred[pos]!!))
                }
            }
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
    }

    private val INFERRERS = hashMap(
            Pair("nullability", NullabilityInferrer() as AnnotationInferrer<Annotation>),
            Pair("mutability", MUTABILITY_INFERRER as AnnotationInferrer<Annotation>)
    )

    private fun doInferenceTest(testedJarSubstring: String) {
        var currentMethod: Method? = null
        val progressMonitor = object : ProgressMonitor() {
            override fun processingStepStarted(method: Method) {
                currentMethod = method
            }
        }

        val jars = findJarsInLibFolder().filter { f -> f.getName().contains(testedJarSubstring) }
        Assert.assertEquals("Test failed to find exactly one jar file with request '$testedJarSubstring'", jars.size, 1);

        val annotationFiles = ArrayList<File>()
        File("lib").recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })

        val jar = jars.first()
        println("start: $jar")
        try {
            val annotationsMap = inferAnnotations(FileBasedClassSource(arrayList(jar)), annotationFiles, INFERRERS, progressMonitor, false)

            for ((testName, annotations) in annotationsMap) {
                val expectedFile = File("testData/inferenceData/integrated/$testName/${jar.getName()}.annotations.txt")
                val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
                outFile.getParentFile()!!.mkdirs()

                checkConflicts(File(expectedFile.getPath().removeSuffix(".txt") + ".conflicts.txt"), annotations)

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

            println("success")
        } catch (e: Throwable) {
            System.err.println("Working on $currentMethod")
            e.printStackTrace()
            fail();
        }
    }

    fun testAsmDebugAll() = doInferenceTest("asm-debug-all-4.0.jar")
    fun testCollectionsGeneric() = doInferenceTest("collections-generic-4.01.jar")
    fun testColtSrc() = doInferenceTest("colt-1.2.0-src.jar")
    fun testColt() = doInferenceTest("colt-1.2.0.jar")
    fun testConcurrentSrc() = doInferenceTest("concurrent-1.3.4-src.jar")
    fun testConcurrent() = doInferenceTest("concurrent-1.3.4.jar")
    fun testGsCollectionsSrc() = doInferenceTest("gs-collections-2.0.0-sources.jar")
    fun testGsCollections() = doInferenceTest("gs-collections-2.0.0.jar")
    fun testGsCollectionsApiSrc() = doInferenceTest("gs-collections-api-2.0.0-sources.jar")
    fun testGsCollectionsApi() = doInferenceTest("gs-collections-api-2.0.0.jar")
    fun testGuava() = doInferenceTest("guava-13.0.1.jar")
    fun testJ3DCore() = doInferenceTest("j3d-core-1.3.1.jar")
    fun testJung3DSrc() = doInferenceTest("jung-3d-2.0.1-sources.jar")
    fun testJung3D() = doInferenceTest("jung-3d-2.0.1.jar")
    fun testJung3dDemosSrc() = doInferenceTest("jung-3d-demos-2.0.1-sources.jar")
    fun testJung3dDemos() = doInferenceTest("jung-3d-demos-2.0.1.jar")
    fun testJungAlgorithmsSrc() = doInferenceTest("jung-algorithms-2.0.1-sources.jar")
    fun testJungAlgorithms() = doInferenceTest("jung-algorithms-2.0.1.jar")
    fun testJungApiSrc() = doInferenceTest("jung-api-2.0.1-sources.jar")
    fun testJungApi() = doInferenceTest("jung-api-2.0.1.jar")
    fun testJungGraphImplSrc() = doInferenceTest("jung-graph-impl-2.0.1-sources.jar")
    fun testJungGraphImpl() = doInferenceTest("jung-graph-impl-2.0.1.jar")
    fun testJungIoSrc() = doInferenceTest("jung-io-2.0.1-sources.jar")
    fun testJungIo() = doInferenceTest("jung-io-2.0.1.jar")
    fun testJungJaiSrc() = doInferenceTest("jung-jai-2.0.1-sources.jar")
    fun testJungJai() = doInferenceTest("jung-jai-2.0.1.jar")
    fun testJungJaiSamplesSrc() = doInferenceTest("jung-jai-samples-2.0.1-sources.jar")
    fun testJungJaiSamples() = doInferenceTest("jung-jai-samples-2.0.1.jar")
    fun testJungSamplesSrc() = doInferenceTest("jung-samples-2.0.1-sources.jar")
    fun testJungSamples() = doInferenceTest("jung-samples-2.0.1.jar")
    fun testJungVisualizationSrc() = doInferenceTest("jung-visualization-2.0.1-sources.jar")
    fun testJungVisualization() = doInferenceTest("jung-visualization-2.0.1.jar")
    fun testJUnitSrc() = doInferenceTest("junit-4.10-src.jar")
    fun testJUnit() = doInferenceTest("junit-4.10.jar")
    fun testStaxApi() = doInferenceTest("stax-api-1.0.1.jar")
    fun testVecmath() = doInferenceTest("vecmath-1.3.1.jar")
    fun testWstxAsl() = doInferenceTest("wstx-asl-3.2.6.jar")
}