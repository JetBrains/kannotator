package org.jetbrains.kannotator.client.sdk

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
    val jarFile = File(args[0])
    val inDir = File(args[1])

    val outputDir = File(args[2])
    val jaifName = args[3]

    outputDir.deleteRecursively()
    outputDir.mkdir()
    annotateSDK(jarFile, inDir, outputDir, jaifName)
}

/* annotates SDK (JDK or android sdk)
 * jarFile - bytecode to annotated
 * inDir - directory with customization information (see below)
 * outputDir - directory where annotations in xml format will be placed
 * jaifName - jaif will be out/artifacts/annotations/${jaifName}.jaif
 *
 * this script assumes that customization dir has following (see jdk-custom/README.md):
 * - interestingPackages.txt
 * - includedClassNames.txt
 * - propagationOverrides.txt
 * - exceptions.txt
 * - annotations/...
 */
fun annotateSDK(jarFile: File, inDir: File, outputDir: File, jaifName: String) {
    val jarSource = FileBasedClassSource(listOf(jarFile))
    val declarationIndex = DeclarationIndexImpl(jarSource)

    // settings
    val outDirJAIF = File("out/artifacts/annotations")
    outDirJAIF.mkdirs()

    val existingAnnotationsDir = File(inDir, "annotations")
    val kotlinSignaturesDir = File(inDir, "annotations")

    val interestingPackagesFile = File(inDir, "interestingPackages.txt")
    val includedClassNamesFile = File(inDir, "includedClassNames.txt")
    val propagationOverridesFile = File(inDir, "propagationOverrides.txt")
    val exceptionsFile = File(inDir, "exceptions.txt")

    val interestingPackages =
            if (interestingPackagesFile.exists())
                BufferedReader(FileReader(interestingPackagesFile)) use { p ->p.lineIterator().toSet() }
            else
                setOf<String>()

    val includedClassNames =
            if (includedClassNamesFile.exists())
                BufferedReader(FileReader(includedClassNamesFile)) use { p -> p.lineIterator().toSet()}
            else
                setOf<String>()

    val packageFilter =
            if (interestingPackages.empty)
                {(pkg: String) -> true}
            else
                {(pkg: String) -> interestingPackages.any { interestingPkg -> pkg == interestingPkg || pkg.startsWith("$interestingPkg/")} }

    val nullabilityPropagationOverrides : Annotations<NullabilityAnnotation> =
            loadAnnotationsFromLogs(arrayListOf(propagationOverridesFile), declarationIndex)

    val conflictExceptions =
            loadPositionsOfConflictExceptions(declarationIndex, exceptionsFile)

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
            progressMonitor = SDKProgressIndicator()
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
            fileName = jaifName,
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

class SDKProgressIndicator() : FileAwareProgressMonitor() {

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
