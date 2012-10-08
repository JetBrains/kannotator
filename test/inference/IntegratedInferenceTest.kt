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

class IntegratedInferenceTest : TestCase() {
    fun test() {
        val jars = findJarFiles(arrayList(File("lib")))
//        val jars = arrayList(File("/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Classes/classes.jar"))
//        val jars = arrayList(File("lib/junit-4.10.jar"))

        var errors = false

        for (jar in jars) {
            println("start: $jar")
            var currentMethod: Method? = null
            try {
                val outFile = File("testData/inferenceData/integrated/${jar.getName()}.annotations.txt")
                outFile.getParentFile()!!.mkdirs()

                val inferred = inferNullabilityAnnotations(arrayList(jar), Collections.emptyList(),
                        object : ProgressMonitor() {
                            override fun processingStepStarted(method: Method) {
                                currentMethod = method
                            }
                        })

                PrintStream(FileOutputStream(outFile)) use {
                    p ->
                    inferred forEach {
                        pos, ann ->
                        p.println(pos.toAnnotationKey())
                        p.println("$ann")
                    }
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
}