package org.jetbrains.kannotator.client.android

import org.jetbrains.kannotator.*
import org.jetbrains.kannotator.annotations.io.*
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.*
import org.jetbrains.kannotator.main.*

import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation

import kotlinlib.recurseFiltered
import kotlinlib.deleteRecursively
import kotlinlib.invertedRecurse

import java.io.File
import java.io.PrintStream
import java.util.ArrayList
import java.util.Date
import java.io.FileReader
import java.io.BufferedReader

fun main(args: Array<String>) {
    annotateAndroidSDK()
}

// TODO: include only those methods that appear in android.jar (that is, API)
fun annotateAndroidSDK() {

    // settings

    val sdkJarFile = "android/android-sdk_4.4.2.jar"
    val existingAnnotationsDir = File("android/annotations")
    val kotlinSignaturesDir = File("android/annotations")
    val outDir = "android-sdk-annotations-snapshot"
    //val interestingPackages = setOf("java", "javax", "org")
    val propagationOverridesFile = File("android/propagationOverrides.txt")
    val outDirJAIF = File("out/artifacts/annotations")

    outDirJAIF.mkdirs()

    val outputDir = File(outDir)
    outputDir.deleteRecursively()
    outputDir.mkdir()

    val includedClassNames = BufferedReader(FileReader(File("jdk-custom/includedClassNames.txt"))) use { p ->
        p.lineIterator().toSet()
    }

    val packageFilter = { (pkg: String) -> true }

    val jarSource = FileBasedClassSource(listOf(File(sdkJarFile)))
    val declarationIndex = DeclarationIndexImpl(jarSource)


    val nullabilityPropagationOverrides : Annotations<NullabilityAnnotation> =
            loadAnnotationsFromLogs(arrayListOf(propagationOverridesFile), declarationIndex)

    val conflictExceptions =
            loadPositionsOfConflictExceptions(declarationIndex, File("jdk-custom/exceptions.txt"))

    val xmlAnnotations = ArrayList<File>()

    existingAnnotationsDir.recurseFiltered({ it.isFile() && it.getName().endsWith(".xml") }, {
        xmlAnnotations.add(it)
    })

    val inferrers =
            mapOf<AnalysisType, AnnotationInferrer<Any, Qualifier>>(
                    NULLABILITY_KEY to NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>,
                    MUTABILITY_KEY to MUTABILITY_INFERRER as AnnotationInferrer<Any, Qualifier>
            )

    val inferenceResult = inferAnnotations(
            classSource = jarSource,
            existingAnnotationFiles = xmlAnnotations,
            inferrers = inferrers,
            errorHandler = NO_ERROR_HANDLING,
            loadOnly = false,
            propagationOverrides =
            hashMapOf(NULLABILITY_KEY to nullabilityPropagationOverrides, MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
            existingAnnotations =
            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
            packageIsInteresting = packageFilter,
            existingPositionsToExclude = mapOf(),
            progressMonitor = ProgressIndicator()
    )

    val nullability: InferenceResultGroup<NullabilityAnnotation>  =
            inferenceResult.groupByKey[NULLABILITY_KEY]!! as InferenceResultGroup<NullabilityAnnotation>
    val nullabilityInferred = nullability.inferredAnnotations
    val nullabilityExisting = nullability.existingAnnotations

    val nullabilityConflicts =
            processAnnotationInferenceConflicts(
                    inferredAnnotations = nullabilityInferred as MutableAnnotations,
                    existingAnnotations = nullabilityExisting,
                    inferrer = NullabilityInferrer(),
                    positionsOfConflictExceptions = conflictExceptions
            )

    writeAnnotationsToJaif(
            declarationIndex,
            destRoot = outDirJAIF,
            fileName = "kotlin-android-sdk-annotations",
            nullability = nullability.inferredAnnotations,
            propagatedNullabilityPositions = nullability.propagatedPositions,
            includeNullable = true
    )

    writeAnnotationsToXMLByPackage(
            keyIndex = declarationIndex,
            declIndex = declarationIndex,
            srcRoot = kotlinSignaturesDir,
            destRoot = outputDir,
            nullability = nullabilityInferred,
            propagatedNullabilityPositions = setOf(), // propagated annotations are not marked in "annotations.xml"
            includedClassNames = includedClassNames,
            errorHandler = simpleErrorHandler {
                kind, message ->
                throw IllegalArgumentException(message)
            },
            includeOnlyMethods = true,
            packageIsInteresting = packageFilter
    )

    check(nullabilityConflicts.empty,
            """There should be no unresolved conflicts in annotations.
            There are 2 options to resolve this situation:
              1) modify existing (input) annotations
              2) modify exceptions.txt
            Found ${nullabilityConflicts.size()} conflicts:
            ${nullabilityConflicts.makeString("\n")}
            """)
}

class ProgressIndicator() : FileAwareProgressMonitor() {

    var numberOfMethods = 0
    var numberOfProcessedMethods = 0
    var progressPercent = -1

    fun logVerbose(msg: String) {
        println("${Date()} ${msg}")
    }

    override fun processingStarted() {
        logVerbose("Started...")
        numberOfProcessedMethods = 0
    }

    override fun annotationIndexLoaded(index: AnnotationKeyIndex) {
        logVerbose("Index loaded")
    }

    override fun methodsProcessingStarted(methodCount: Int) {
        numberOfMethods = methodCount
        logVerbose("Found ${methodCount} methods")
    }

    override fun processingComponentFinished(methods: Collection<Method>) {
        numberOfProcessedMethods += methods.size

        if (numberOfMethods != 0) {
            val currentProgressPercent = (numberOfProcessedMethods.toDouble() / numberOfMethods * 100).toInt()
            if (currentProgressPercent != progressPercent) {
                progressPercent = currentProgressPercent
                logVerbose("  Inferring: $currentProgressPercent% \r");
            }
        } else {
            logVerbose("  Inferring: 100% \n");
        }
    }
}
