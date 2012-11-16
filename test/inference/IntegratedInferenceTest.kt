package inference

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import java.util.Collections
import java.io.PrintStream
import java.io.FileOutputStream
import kotlinlib.println
import org.jetbrains.kannotator.annotations.io.toAnnotationKey
import util.findJarFiles
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

    private fun doInferenceTest(testMap: Map<Any, AnnotationInferrer<Any>>) {
        var currentMethod: Method? = null
        val progressMonitor = object : ProgressMonitor() {
            override fun processingStepStarted(method: Method) {
                currentMethod = method
            }
        }

        val annotationFiles = ArrayList<File>()
        File("lib").recurseFiltered({f -> f.isFile() && f.getName().endsWith(".xml")}, {f -> annotationFiles.add(f)})

        val jars = findJarFiles(arrayList(File("lib"))).filter {f -> f.getName() != "kotlin-runtime.jar"}

        var errors = false

        for (jar in jars) {
            println("start: $jar")
            try {
                val annotationsMap = inferAnnotations(arrayList(jar), annotationFiles, testMap, progressMonitor, false)

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
                errors = true
            }
        }

        if (errors) fail("There were errors, see the output")
    }

    fun test() = doInferenceTest(
            hashMap(
                    Pair("nullability", NULLABILITY_INFERRER as AnnotationInferrer<Any>),
                    Pair("mutability", MUTABILITY_INFERRER as AnnotationInferrer<Any>)
            )
    )
}