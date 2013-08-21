package org.jetbrains.kannotator.client

import java.io.File
import java.io.PrintStream
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.annotations.io.AnnotationsFormat
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.*
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.simpleErrorHandler
import org.jetbrains.kannotator.annotations.io.InferringTaskTrait
import org.jetbrains.kannotator.annotations.io.InferringParameters
import org.jetbrains.kannotator.client.ConsoleInferringTask.TextInferringProgressIndicator
import org.jetbrains.kannotator.annotations.io.AnnotationTaskProgressMonitor

public class ConsoleInferringTask(taskParams: InferringParameters, diagnostics: PrintStream) :
InferringTaskTrait<InferringParameters, TextInferringProgressIndicator>
{
    class TextInferringProgressIndicator(val parameters: InferringParameters, val diagnostics: PrintStream) : AnnotationTaskProgressMonitor() {
        val totalAmountOfJars: Int = parameters.libToFiles.values().fold(0, { sum, files -> sum + files.size })
        var numberOfJarsFinished: Int = 0
        var numberOfMethods = 0
        var numberOfProcessedMethods = 0

        fun logAlways(string: String) = diagnostics.println(string)
        fun logVerbose(string: String) = if (parameters.verbose) diagnostics.println(string)
        fun logVerboseNoNewline(string: String) = if (parameters.verbose) diagnostics.print(string)

        override fun jarProcessingStarted(fileName: String, libraryName: String) {

        }

        override fun processingStarted() {
            logVerbose("\nFile: ${numberOfJarsFinished + 1} / $totalAmountOfJars.");
            logVerbose("Initializing...")
            numberOfProcessedMethods = 0
        }

        override fun methodsProcessingStarted(methodCount: Int) {
            numberOfMethods = methodCount
        }

        override fun processingComponentFinished(methods: Collection<Method>) {
            numberOfProcessedMethods += methods.size

            if (numberOfMethods != 0) {
                val progressPercent = (numberOfProcessedMethods.toDouble() / numberOfMethods * 100).toInt()
                logVerboseNoNewline("Inferring: $progressPercent% \r");
            }
            else {
                logVerbose("Inferring: 100% \n");
            }
        }
        override  fun processingFinished() {
            numberOfJarsFinished++
        }

        override fun processingAborted() {
            logVerbose("Annotating was canceled")
        }
        //All libraries are annotated
        fun annotationFinished() {
            val numberOfFiles = parameters.libToFiles.size
            val message = when(numberOfFiles)
            {
                0 -> "no files were annotated"
                1 -> "one file was annotated"
                else -> "$numberOfFiles files were annotated"
            }
            logVerbose("Annotating finished successfully: " + message)
        }
    }


    public override val parameters: InferringParameters;
    {
        parameters = taskParams;
    }
    public override var monitor: TextInferringProgressIndicator? = TextInferringProgressIndicator(parameters, diagnostics)


    override fun afterProcessing(params: InferringParameters) {
        monitor!!.annotationFinished()
    }
    override fun beforeFile(params: InferringParameters, filename: String, libname: String) {
        monitor!!.jarProcessingStarted(filename, libname)
    }

}