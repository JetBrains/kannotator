package inference

import junit.framework.TestCase
import junit.framework.Assert.*
import java.io.File
import org.jetbrains.kannotator.main.inferNullabilityAnnotations
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
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import java.util.ArrayList
import util.assertEqualsOrCreate
import kotlinlib.recurseFiltered
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.declarations.AnnotationPosition
import kotlin.test.assertTrue
import org.jetbrains.kannotator.declarations.Annotations

class IntegratedInferenceTest : TestCase() {
    private fun checkConflicts(conflictFile: File, inferred: Annotations<NullabilityAnnotation>) {
        val existingAnnotations = (inferred as AnnotationsImpl<NullabilityAnnotation>).delegate
        if (existingAnnotations != null) {
            val conflicts = ArrayList<Triple<AnnotationPosition, NullabilityAnnotation, NullabilityAnnotation>>()
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
            }
            assertTrue("Found annotation conflicts", conflicts.isEmpty())
        }
    }

    fun test() {
        val annotationFiles = ArrayList<File>()
        File("lib").recurseFiltered({f -> f.isFile() && f.getName().endsWith(".xml")}, {f -> annotationFiles.add(f)})

        val jars = findJarFiles(arrayList(File("lib"))).filter {f -> f.getName() != "kotlin-runtime.jar"}

        var errors = false

        for (jar in jars) {
            println("start: $jar")
            var currentMethod: Method? = null
            try {
                val expectedFile = File("testData/inferenceData/integrated/${jar.getName()}.annotations.txt")
                val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
                outFile.getParentFile()!!.mkdirs()

                val inferred = inferNullabilityAnnotations(arrayList(jar), annotationFiles,
                        object : ProgressMonitor() {
                            override fun processingStepStarted(method: Method) {
                                currentMethod = method
                            }
                        },
                        false
                )

                val map = TreeMap<String, NullabilityAnnotation>()
                inferred forEach {
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

                checkConflicts(File(expectedFile.getPath().removeSuffix(".txt") + ".conflict.txt"), inferred)

                println("success")
            } catch (e: Throwable) {
                System.err.println("Working on $currentMethod")
                e.printStackTrace()
                errors = true
            }
        }

        if (errors) fail("There were errors, see the output")
    }
}