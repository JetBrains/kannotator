package org.jetbrains.kannotator.cli

import java.io.FileWriter
import annotations.io.writeAnnotationsJSRStyleGroupedByPackage
import org.jetbrains.kannotator.annotationsInference.nullability.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.*
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.*
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import java.util.Collections
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.annotations.io.buildAnnotationsDataMap

//TODO: to build a proper CLI use some argument parsing library
fun main(args: Array<String>) {
    val argsList = ArrayList(args.toList())
    if (argsList.size < 2 || argsList[0] == "-h" || argsList[0] == "-help" || argsList.size > 3) {
        printUsage()
        return
    }
    var xmlFormat = true
    if (argsList[0] == "--jaif") {
        xmlFormat = false
    }
    if (argsList[0] == "--jaif" || argsList[0] == "--xml") {
        argsList.remove(0)
    }
    val jarPath = argsList[0]
    val jarFile = File(jarPath)
    val outputDirPath = argsList[1]
    if (!jarFile.isFile()) {
        reportError("$jarPath is not a file")
        return
    }

    try {
        val inferrerMap = HashMap<AnalysisType, AnnotationInferrer<Any, Qualifier>>()
        inferrerMap[NULLABILITY_KEY] = NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>
        inferrerMap[MUTABILITY_KEY] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any, Qualifier>

        // TODO: Add existing annotations from dependent libraries
        val inferenceResult = inferAnnotations(
                FileBasedClassSource(arrayListOf(jarFile)), ArrayList<File>(),
                inferrerMap,
                object : ProgressMonitor() {

                    var processedMethods = 0
                    var totalMethodsToProcess = 0
                    var lastReported = 0

                    override fun methodsProcessingStarted(methodCount: Int) {
                        totalMethodsToProcess = methodCount
                        println("Started processing.")
                        println("Total methods to process: $methodCount.")
                    }

                    override fun processingComponentFinished(methods: Collection<Method>) {
                        processedMethods += methods.size
                        if (totalMethodsToProcess != 0 && (processedMethods - lastReported) > totalMethodsToProcess / 12) {
                            println("Processed $processedMethods out of $totalMethodsToProcess methods.")
                            lastReported = processedMethods
                        }
                    }

                    override fun processingFinished() {
                        if (lastReported != totalMethodsToProcess) {
                            println("Proccessed $totalMethodsToProcess methods.")
                        }
                        println("Finished processing.")
                    }
                },
                false,
                false,
                hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
                hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
                { true },
                Collections.emptyMap()
        )

        val inferredNullabilityAnnotations: Annotations<NullabilityAnnotation> =
                checkNotNull(
                        inferenceResult.groupByKey[NULLABILITY_KEY]!!.inferredAnnotations,
                        "Only nullability annotations are supported by now") as
                Annotations<NullabilityAnnotation>

        val propagatedNullabilityPositions: Set<AnnotationPosition> =
                checkNotNull(
                        inferenceResult.groupByKey[NULLABILITY_KEY]!!.propagatedPositions,
                        "Only nullability annotations are supported by now"
                )

        val fileBasedClassSource: FileBasedClassSource = FileBasedClassSource(arrayListOf(jarFile))
        val declarationIndex: DeclarationIndexImpl = DeclarationIndexImpl(fileBasedClassSource, { null }, true)

        File(outputDirPath).mkdirs()
        if (xmlFormat) {
            writeAnnotationsToXMLByPackage(
                    declarationIndex,
                    declarationIndex,
                    null,
                    File(outputDirPath),
                    inferredNullabilityAnnotations,
                    propagatedNullabilityPositions)
        } else {
            val dataMap = buildAnnotationsDataMap(declarationIndex, inferredNullabilityAnnotations,
                    propagatedNullabilityPositions,
                    Collections.emptySet(), Collections.emptySet(), Collections.emptySet())
            writeAnnotationsJSRStyleGroupedByPackage(FileWriter(File(outputDirPath, "annotations.jaif")), dataMap)
        }

    } catch (e: Throwable) {
        println("ERROR:")
        println(e.getMessage())
        return
    }
    println("Successfully written annotations")
}

fun printUsage() {
    println("Usage: <keys> <path_to_jar> <path_to_output_dir>.")
    println("Possible keys are: --xml to write output in xml format, --jaif to write output in jaif format. Default is xml.")
}

fun reportError(text: String) {
    println("Error: $text.")
    printUsage()
}