package org.jetbrains.kannotator.client.jdk

import org.jetbrains.kannotator.annotations.io.*

import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.ClassSource

import java.io.File
import java.util.ArrayList
import kotlinlib.recurseFiltered

import org.jetbrains.kannotator.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.*
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.InferenceResultGroup
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.declarations.AnnotationsImpl
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER
import kotlin.util.measureTimeMillis
import kotlinlib.deleteRecursively

// build annotations for jdk
fun main(args: Array<String>) {
    val jdkPackageFilter = { (name : String) ->
        name.startsWith("java/") || name.startsWith("javax.") || name.startsWith("org.")
    }
    annotateJar("lib/jdk/jre-7u12-windows-rt.jar", null, jdkPackageFilter, "jdk-annotations-snapshot")
}

fun annotateJar(jarFile : String, existingAnnotationsDir : String?, packageFilter: (String) -> Boolean, outDir : String) {

    val outputDir = File(outDir)
    outputDir.deleteRecursively()
    outputDir.mkdir()

    val jarSource = FileBasedClassSource(listOf(File(jarFile)))

    val xmlAnnotations = ArrayList<File>()
    if (existingAnnotationsDir != null) {
        File(existingAnnotationsDir).recurseFiltered({ it.isFile() && it.getName().endsWith(".xml") }, { xmlAnnotations.add(it) })
    }

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
            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
            existingAnnotations =
            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
            packageIsInteresting = packageFilter,
            existingPositionsToExclude = mapOf()
    )

    val annotations = inferenceResult.groupByKey[NULLABILITY_KEY]!!

    val inferredNullabilityAnnotations =
            annotations.inferredAnnotations as Annotations<NullabilityAnnotation>

    val propagatedNullabilityPositions =
            annotations.propagatedPositions

    val declarationIndex = DeclarationIndexImpl(jarSource)

    writeAnnotationsToXMLByPackage(
            keyIndex = declarationIndex,
            declIndex = declarationIndex,
            srcRoot = null,
            destRoot = outputDir,
            nullability = inferredNullabilityAnnotations,
            propagatedNullabilityPositions = propagatedNullabilityPositions,
            errorHandler = simpleErrorHandler {
                kind, message ->
                throw IllegalArgumentException(message)
            })
}
