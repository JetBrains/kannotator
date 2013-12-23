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

import java.io.File
import java.util.ArrayList
import kotlinlib.recurseFiltered
import kotlinlib.deleteRecursively
import kotlin.util.measureTimeMillis

fun main(args: Array<String>) {

    val start = System.currentTimeMillis()
    annotateJDK()
    val time = (System.currentTimeMillis() - start) / 1000

    println("time: ${time} secs")
}

fun annotateJDK() {

    val jdkJarFile = "lib/jdk/jre-7u12-windows-rt.jar"

    val existingAnnotationsDir = File("lib/jdk-annotations")

    val kotlinSignaturesDir = File("lib/jdk-annotations")
    val outDir = "jdk-annotations-snapshot"
    // annotations for these classes should be included to make class hierarchy consistent
    // from Kotlin point of view
    // TODO: incorporate this logic - find such classes in hierarchy and include them automatically
    val classesToInclude = setOf(
            "java/lang/AbstractStringBuilder"
    )

    val packageFilter = { (name : String) ->
        name.startsWith("java/lang/") //|| name.startsWith("javax.") || name.startsWith("org.")
    }

    val jarSource = FileBasedClassSource(listOf(File(jdkJarFile)))
    val declarationIndex = DeclarationIndexImpl(jarSource)

    val propagationOverridesFile =
            File("testData/inferenceData/integrated/nullability/propagationOverrides.txt")
    val nullabilityPropagationOverrides : Annotations<NullabilityAnnotation> =
            loadAnnotationsFromLogs(arrayListOf(propagationOverridesFile), declarationIndex!!)

    val outputDir = File(outDir)
    outputDir.deleteRecursively()
    outputDir.mkdir()


    val xmlAnnotations = ArrayList<File>()
    if (existingAnnotationsDir != null) {
        existingAnnotationsDir.recurseFiltered({ it.isFile() && it.getName().endsWith(".xml") }, {
            xmlAnnotations.add(it)
        })
    }

    // TODO: try without mutability inferrers
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
            existingPositionsToExclude = mapOf()
    )

    val nullability: InferenceResultGroup<NullabilityAnnotation>  =
            inferenceResult.groupByKey[NULLABILITY_KEY]!! as InferenceResultGroup<NullabilityAnnotation>
    val nullabilityInferred = nullability.inferredAnnotations
    val nullabilityExisting = nullability.existingAnnotations

    val nullabilityConflicts =
            processAnnotationInferenceConflicts(
                    nullabilityInferred as MutableAnnotations,
                    nullabilityExisting,
                    NullabilityInferrer()
            )
    println("conflicts: ${nullabilityConflicts.size()}")
    println("${nullabilityConflicts}")

    writeJdkAnnotations(
            keyIndex = declarationIndex,
            declIndex = declarationIndex,
            kotlinSignaturesDir = kotlinSignaturesDir,
            destRoot = outputDir,
            nullability = nullabilityInferred,
            propagatedNullabilityPositions = setOf(), // we do not store this information in jdk annotations
            includedClassNames = classesToInclude,
            errorHandler = simpleErrorHandler {
                kind, message ->
                throw IllegalArgumentException(message)
            })
}
