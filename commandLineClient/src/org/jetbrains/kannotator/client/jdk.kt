package org.jetbrains.kannotator.client.jdk

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
    annotateJDK()
}

fun annotateJDK() {

    // settings

    val jdkJarFile = "lib/jdk/jre-7u12-windows-rt.jar"
    val existingAnnotationsDir = File("jdk-custom/jdk-annotations")
    val kotlinSignaturesDir = File("jdk-custom/jdk-annotations")
    val outDir = "jdk-annotations-snapshot"
    val interestingPackages = setOf("java", "javax", "org")

    val outputDir = File(outDir)
    outputDir.deleteRecursively()
    outputDir.mkdir()

    // annotations for these classes should be included to make class hierarchy consistent
    // from Kotlin point of view
    // TODO: do we really need it?
    // TODO: incorporate this logic - find such classes in hierarchy and include them automatically
    val includedClassNames = BufferedReader(FileReader(File("jdk-custom/includedClassNames.txt"))) use { p ->
        p.lineIterator().toSet()
    }

    val packageFilter = { (name: String) -> interestingPackages.any { name.startsWith("$it/")} }

    val jarSource = FileBasedClassSource(listOf(File(jdkJarFile)))
    val declarationIndex = DeclarationIndexImpl(jarSource)

    val propagationOverridesFile = File("jdk-custom/propagationOverrides.txt")
    val nullabilityPropagationOverrides : Annotations<NullabilityAnnotation> =
            loadAnnotationsFromLogs(arrayListOf(propagationOverridesFile), declarationIndex)

    val conflictExceptions =
            loadPositionsOfConflictExceptions(declarationIndex, File("jdk-custom/exceptions.txt"))

    val xmlAnnotations = ArrayList<File>()

    existingAnnotationsDir.recurseFiltered({ it.isFile() && it.getName().endsWith(".xml") }, {
        xmlAnnotations.add(it)
    })

    // TODO: try without mutability inferrers - possibly faster inference,
    // does mutability inference really help to infer nullability in the case of jdk annotations?
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
            progressMonitor = JDKProgressIndicator()
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

    check(nullabilityConflicts.empty,
            """There should be no unresolved conflicts in annotations.
            There are 2 options to resolve this situation:
              1) modify existing (input) annotations
              2) modify exceptions.txt
            Found ${nullabilityConflicts.size()} conflicts:
            ${nullabilityConflicts.makeString("\n")}
            """)

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
            includeOnlyMethods = true
    )

    // current annotations writers dump all packages, including not interesting
    // deleting not interesting packages
    outputDir.invertedRecurse { file ->
        val path = file.getPath()
        val interestingFolders = interestingPackages.map {"$outDir/$it"}
        val interesting = interestingFolders.any { path == it || path.startsWith("$it/") }
        if (!interesting) {
            file.delete()
        }
    }
}

class JDKProgressIndicator() : FileAwareProgressMonitor() {

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
