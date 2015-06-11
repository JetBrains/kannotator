package org.jetbrains.kannotator.client

import java.io.PrintStream
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.annotations.io.FileAwareProgressMonitor
import org.jetbrains.kannotator.annotations.io.InferenceParams

class PrintStreamProgressIndicator(val parameters: InferenceParams,
                                   val diagnostics: PrintStream) : FileAwareProgressMonitor() {

    val totalAmountOfJars: Int = parameters.libraries.fold(0, { sum, annotatedLib -> sum + annotatedLib.files.size() })
    var numberOfJarsFinished: Int = 0
    var numberOfMethods = 0
    var numberOfProcessedMethods = 0

    fun logAlways(string: String) = diagnostics.println(string)
    fun logVerbose(string: String) = if (parameters.verbose) diagnostics.println(string)
    fun logVerboseNoNewline(string: String) = if (parameters.verbose) diagnostics.print(string)

    override fun processingStarted() {
        logVerbose("  File: ${numberOfJarsFinished + 1} / $totalAmountOfJars.");
        logVerbose("  Initializing...")
        numberOfProcessedMethods = 0
    }

    override fun methodsProcessingStarted(methodCount: Int) {
        numberOfMethods = methodCount
    }

    override fun processingComponentFinished(methods: Collection<Method>) {
        numberOfProcessedMethods += methods.size()

        if (numberOfMethods != 0) {
            val progressPercent = (numberOfProcessedMethods.toDouble() / numberOfMethods * 100).toInt()
            logVerboseNoNewline("  Inferring: $progressPercent% \r");
        } else {
            logVerbose("  Inferring: 100% \n");
        }
    }
    override  fun processingFinished() {
        numberOfJarsFinished++
    }

    override fun processingAborted() {
        logVerbose("Annotating was canceled")
    }

    //All libraries are annotated
    override fun allFilesAreAnnotated() {
        val numberOfFiles = parameters.libraries.count()
        val message = when(numberOfFiles) {
            0 -> "no files were annotated"
            1 -> "one file was annotated"
            else -> "$numberOfFiles files were annotated"
        }
        logVerbose("Annotating finished successfully: " + message)
    }

    override fun jarProcessingStarted(fileName: String, libraryName: String) {
        logVerbose("Processing $fileName")
    }

    override fun jarProcessingFinished(fileName: String, libraryName: String) {
    }
}
