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

class IntegratedInferenceTest : TestCase() {
    fun test() {
        val jars = findJarFiles(arrayList(File("lib"))).filter {f -> f.getName() != "kotlin-runtime.jar"}

        var errors = false

        for (jar in jars) {
            println("start: $jar")
            var currentMethod: Method? = null
            try {
                val expectedFile = File("testData/inferenceData/integrated/${jar.getName()}.annotations.txt")
                val outFile = File(expectedFile.getPath().removeSuffix(".txt") + ".actual.txt")
                outFile.getParentFile()!!.mkdirs()

                val inferred = inferNullabilityAnnotations(arrayList(jar), Collections.emptyList(),
                        object : ProgressMonitor() {
                            override fun processingStepStarted(method: Method) {
                                currentMethod = method
                            }
                        })

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