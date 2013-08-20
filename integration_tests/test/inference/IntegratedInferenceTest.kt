package inference

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import java.util.Collections
import java.io.PrintStream
import java.io.FileOutputStream
import kotlinlib.*
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.declarations.Method
import java.io.FileInputStream
import java.util.TreeMap
import org.jetbrains.kannotator.controlFlow.builder.analysis.Annotation
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.util.ArrayList
import util.assertEqualsOrCreate
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.AnnotationPosition
import kotlin.test.assertTrue
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.main.*
import util.findJarsInLibFolder
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.junit.Assert
import util.*
import java.util.HashSet
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXML
import java.io.FileWriter
import org.jetbrains.kannotator.declarations.PositionsForMethod
import org.jetbrains.kannotator.annotations.io.AnnotationDataImpl
import org.jetbrains.kannotator.kotlinSignatures.renderMethodSignature
import org.jetbrains.kannotator.kotlinSignatures.kotlinSignatureToAnnotationData
import java.io.StringWriter
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.index.DeclarationIndex
import kotlinSignatures.KotlinSignatureTestData.MutabilityNoAnnotations
import java.io.BufferedReader
import java.io.FileReader
import org.jetbrains.kannotator.annotations.io.AnnotationData
import org.jetbrains.kannotator.declarations.isPublicOrProtected
import org.jetbrains.kannotator.declarations.isPublic
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import java.util.HashMap
import org.jetbrains.kannotator.declarations.forEachValidPosition
import org.jetbrains.kannotator.annotationsInference.nullability.*
import kotlin.dom.addClass
import java.util.LinkedHashMap
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.ClassMember
import org.jetbrains.kannotator.declarations.getInternalPackageName
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.jetbrains.kannotator.funDependecy.getTopologicallySortedStronglyConnectedComponents
import org.jetbrains.kannotator.annotations.io.methodsToAnnotationsMap
import org.jetbrains.kannotator.annotations.io.getPackageName
import org.jetbrains.kannotator.annotations.io.buildAnnotationsDataMap
import org.jetbrains.kannotator.annotations.io.loadAnnotationsFromLogs
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.funDependecy
import org.jetbrains.kannotator.declarations.isStatic
import org.jetbrains.kannotator.declarations.isProtected
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.NO_ERROR_HANDLING

class IntegratedInferenceTest : TestCase() {
    private fun <A: Any> reportConflicts(
            testName: String,
            conflictFile: File,
            keyIndex: AnnotationKeyIndex,
            inferredAnnotations: Annotations<A>,
            existingAnnotations: Annotations<A>,
            inferrer: AnnotationInferrer<A, *>
    ) {
        val conflictExceptions = loadPositionsOfConflictExceptions(keyIndex, File("testData/inferenceData/integrated/$testName/exceptions.txt"))
        val conflicts = processAnnotationInferenceConflicts(
                inferredAnnotations as MutableAnnotations<A>, existingAnnotations, inferrer, conflictExceptions
        )
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

    private fun doInferenceTest(testedJarSubstring: String, existingAnnotationsDir: String? = "lib", packageIsInteresting: (String) -> Boolean = {true}) {
        var annotationIndex: AnnotationKeyIndex? = null

        val progressMonitor = object : ProgressMonitor() {
            var currentMethod: Method? = null

            override fun annotationIndexLoaded(index: AnnotationKeyIndex) {
                annotationIndex = index
            }

            override fun methodsProcessingStarted(methodCount: Int) {
                println("Total methods: $methodCount")
            }

            override fun processingStepStarted(method: Method) {
//                println(method)
                currentMethod = method
            }
        }

        val jars = findJarsInLibFolder().filter { f -> f.getName().contains(testedJarSubstring) }
        Assert.assertEquals("Test failed to find exactly one jar file with request '$testedJarSubstring'", jars.size, 1);

        val annotationFiles = ArrayList<File>()
        if (existingAnnotationsDir != null) {
            File(existingAnnotationsDir).recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })
        }

        val jar = jars.first()
        println("start: $jar")

        inferAnnotations(FileBasedClassSource(
                arrayListOf(jar)), annotationFiles, INFERRERS, progressMonitor, NO_ERROR_HANDLING, true,
                Collections.emptyMap(), Collections.emptyMap(), {true}, Collections.emptyMap()
        )

        val propagationOverridesFile = File("testData/inferenceData/integrated/nullability/propagationOverrides.txt")
        val propagationOverrides = loadAnnotationsFromLogs(arrayListOf(propagationOverridesFile), annotationIndex!!)

        val inferenceResult = try {
            inferAnnotations(
                    FileBasedClassSource(arrayListOf(jar)),
                    annotationFiles,
                    INFERRERS,
                    progressMonitor,
                    NO_ERROR_HANDLING,
                    false,
                    hashMapOf(NULLABILITY_KEY to propagationOverrides, MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
                    hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
                    packageIsInteresting,
                    Collections.emptyMap()
            )
        }
        catch (e: Throwable) {
            throw IllegalStateException("Failed while working on ${progressMonitor.currentMethod}", e)
        }

        for ((inferrerKey, group) in inferenceResult.groupByKey) {
            val testName = inferrerKey.toString()!!.toLowerCase()
            val expectedFile = File("testData/inferenceData/integrated/$testName/${jar.getName()}.annotations.txt")
            val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
            outFile.getParentFile()!!.mkdirs()

            reportConflicts(
                    testName,
                    File(expectedFile.getPath().removeSuffix(".txt") + ".conflicts.txt"),
                    annotationIndex!!,
                    group.inferredAnnotations,
                    group.existingAnnotations,
                    INFERRERS[inferrerKey]!!
            )

            val map = TreeMap<String, Any>()
            group.inferredAnnotations forEach {
                pos, ann -> map.put(pos.toAnnotationKey(), ann)
            }

            val propagatedKeys = group.propagatedPositions.map { it.toAnnotationKey() }

            PrintStream(FileOutputStream(outFile)) use {
                p ->
                for ((key, ann) in map) {
                    if (key in propagatedKeys) {
                        p.print("@Propagated ")
                    }
                    p.println(key)
                    p.println(ann)
                }
            }

            assertEqualsOrCreate(expectedFile, outFile.readText(), false)

            outFile.delete()
        }

        val nullability = inferenceResult.groupByKey[NULLABILITY_KEY]!!.inferredAnnotations as Annotations<NullabilityAnnotation>
        val mutability = inferenceResult.groupByKey[MUTABILITY_KEY]!!.inferredAnnotations as Annotations<MutabilityAnnotation>

        val file = File("testData/inferenceData/integrated/kotlinSignatures/${jar.getName()}.annotations.xml")

        writeKotlinSignatureAnnotationsToFile(file, nullability, mutability)
    }


    private fun doInferenceAsNotNullTest(testedJarSubstring: String) {
        val jars = findJarsInLibFolder().filter { f -> f.getName().contains(testedJarSubstring) }
        Assert.assertEquals("Test failed to find exactly one jar file with request '$testedJarSubstring'", jars.size, 1);

        val annotationFiles = ArrayList<File>()
        File("lib").recurseFiltered({ f -> f.isFile() && f.getName().endsWith(".xml") }, { f -> annotationFiles.add(f) })

        val jar = jars.first()
        println("start: $jar")

        val classHierarchy = buildClassHierarchyGraph(FileBasedClassSource(arrayListOf(jar)))
        val methodHierarchy = buildMethodHierarchy(classHierarchy)
        val toProcessMethods = arrayListOf<Method>()

        classHierarchy.nodes.forEach {
            clazzNode -> clazzNode.methods.forEach {
                    method -> if (method.isPublicOrProtected()
                                && (methodHierarchy.findNode(method) == null || method.isStatic())) {
                        toProcessMethods.add(method)
                }
        } }

        methodHierarchy.hierarchyNodes.forEach {
            node ->  if ((node.data.access.isPublic()
                        && node.parents.all { p -> !p.parent.data.access.isPublic()})
                        || (node.data.access.isProtected()
                                && node.parents.all { p -> !p.parent.data.access.isProtected()})) {
                    toProcessMethods.add(node.data)
        }}

        val nullability = AnnotationsImpl<NullabilityAnnotation>()
        toProcessMethods.forEach {
            m -> nullability.set(PositionsForMethod(m).forReturnType().position, NullabilityAnnotation.NOT_NULL)
        }

        val mutability = AnnotationsImpl<MutabilityAnnotation>()

        for ((inferrerKey, annotations) in mapOf(Pair(NULLABILITY_KEY, nullability), Pair(MUTABILITY_KEY, mutability) )) {
            val testName = inferrerKey.toString()!!.toLowerCase()
            val expectedFile = File("testData/inferenceData/integrated/$testName/${jar.getName()}.annotations.txt")
            val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
            outFile.getParentFile()!!.mkdirs()

            val map = TreeMap<String, Any>()
            annotations forEach {
                pos, ann -> map.put(pos.toAnnotationKey(), ann!!)
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

        val file = File("testData/inferenceData/integrated/kotlinSignatures/${jar.getName()}.annotations.xml")
        writeKotlinSignatureAnnotationsToFile(file, nullability, mutability)
    }


    fun writeKotlinSignatureAnnotationsToFile(
            expectedFile: File,
            nullability: Annotations<NullabilityAnnotation>,
            mutability: Annotations<MutabilityAnnotation>
    ) {
        val methods = HashSet<Method>()
        nullability.forEach {
            pos, ann ->
            if (pos.member is Method) {
                methods.add(pos.member as Method)
            }
        }

        mutability.forEach {
            pos, ann ->
            if (pos.member is Method) {
                methods.add(pos.member as Method)
            }
        }

        val stringWriter = StringWriter()
        writeAnnotationsToXML(stringWriter,
                methods.sortByToString().map {
                    m ->
                    PositionsForMethod(m).forReturnType().position to arrayListOf(
                            kotlinSignatureToAnnotationData(
                                renderMethodSignature(m, nullability , mutability)
                        )
                    )
                }.toMap()
        )

        val actual = stringWriter.toString()
        assertEqualsOrCreate(expectedFile, actual, true)
    }

    fun testAsmDebugAll() = doInferenceTest("asm-debug-all-4.0.jar")
    fun testCollectionsGeneric() = doInferenceTest("collections-generic-4.01.jar")
    fun testColt() = doInferenceTest("colt-1.2.0.jar")
    fun testConcurrent() = doInferenceTest("concurrent-1.3.4.jar")
    fun testGsCollections() = doInferenceTest("gs-collections-2.0.0.jar")
    fun testGsCollectionsApi() = doInferenceTest("gs-collections-api-2.0.0.jar")
    fun testGuava() = doInferenceTest("guava-13.0.1.jar", existingAnnotationsDir = "testData/inferenceData/integrated/nullability/guava-existing-annotations")
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
    fun testJpsServer() = doInferenceTest("jps-server.jar")
    fun testJDK1_7_0_09_rt_jar() = doInferenceTest("jre-7u12-windows-rt.jar") { name ->
        name.startsWith("java") || name.startsWith("javax") || name.startsWith("org")
    }
    fun testExtensions() = doInferenceTest("extensions.jar")
    fun testDefaultPackage() = doInferenceTest("default-package.jar")
}