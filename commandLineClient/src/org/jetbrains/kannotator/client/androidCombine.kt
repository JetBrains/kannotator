package org.jetbrains.kannotator.client.androidCombine

import java.io.File
import java.util.ArrayList
import kotlinlib.recurseFiltered
import kotlinlib.deleteRecursively

import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.PRINT_TO_CONSOLE
import org.jetbrains.kannotator.annotations.io.parseAnnotations
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.simpleErrorHandler
import org.jetbrains.kannotator.main.InferenceResult
import org.jetbrains.kannotator.controlFlow.builder.analysis.NullabilityKey
import org.jetbrains.kannotator.main.InferenceResultGroup
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.declarations.Annotations
import org.jetbrains.kannotator.declarations.AnnotationPosition
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.declarations.AnnotationsImpl

// we load only nullability annotations here
val inferrers = mapOf(NULLABILITY_KEY to NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>)

val apiJar = FileBasedClassSource(listOf(File("android/android-19-api.jar")))

fun main(args: Array<String>) {

    val studioAnnotationsDir = File("android-sdk-annotations-adt")
    val pluginAnnotationsDir = File("android-sdk-annotations-kotlin-plugin")
    val inferredDir = File("android-sdk-annotations-inferred")
    val outputDir = File("android-sdk-annotations-combined")
    val declarationIndex = DeclarationIndexImpl(apiJar)

    outputDir.deleteRecursively()
    outputDir.mkdir()

    val inferredAnnotations = loadAnnotations(inferredDir)
    val pluginAnnotations = loadAnnotations(pluginAnnotationsDir)
    val studioAnnotations = loadAnnotations(studioAnnotationsDir)

    val diffs = hashSetOf<AnnotationPosition>()

    diffs.addAll(checkAnnotations(inferredAnnotations, pluginAnnotations))
    diffs.addAll(checkAnnotations(inferredAnnotations, studioAnnotations))

    println("Excluding ${diffs.size()} annotations (different from studio annotations)")

    val combinedAnnotations = AnnotationsImpl<NullabilityAnnotation>()

    inferredAnnotations.forEach { pos, ann ->
        if (!diffs.contains(pos)) {
            combinedAnnotations[pos] = ann
        }
    }

    pluginAnnotations.forEach { pos, ann ->
        if (!diffs.contains(pos)) {
            combinedAnnotations[pos] = ann
        }
    }

    studioAnnotations.forEach { pos, ann ->
        if (!diffs.contains(pos)) {
            combinedAnnotations[pos] = ann
        }
    }

    writeAnnotationsToXMLByPackage(
            keyIndex = declarationIndex,
            declIndex = declarationIndex,
            srcRoot = pluginAnnotationsDir,
            destRoot = outputDir,
            nullability = combinedAnnotations,
            propagatedNullabilityPositions = setOf(), // propagated annotations are not marked in "annotations.xml"
            includedClassNames = setOf(),
            errorHandler = simpleErrorHandler {
                kind, message ->
                throw IllegalArgumentException(message)
            },
            includeOnlyMethods = true,
            packageIsInteresting = {true}
    )
}

fun checkAnnotations(annotations1: Annotations<NullabilityAnnotation>, annotations2: Annotations<NullabilityAnnotation>): Set<AnnotationPosition> {
    val diffs = hashSetOf<AnnotationPosition>()
    annotations1.forEach { (pos, ann1) ->
        val ann2 = annotations2.get(pos)
        if (ann2 != null && ann2 != ann1) {
            diffs.add(pos)
        }
    }
    annotations2.forEach { (pos, ann2) ->
        val ann1 = annotations1.get(pos)
        if(ann1 != null && ann2 != ann1) {
            diffs.add(pos)
        }
    }
    return diffs
}

fun File.annotationFiles(): List<File> {
    val xmlAnnotations = ArrayList<File>()
    this.recurseFiltered({ it.isFile() && it.getName().endsWith(".xml") }, {
        xmlAnnotations.add(it)
    })
    return xmlAnnotations
}

fun loadAnnotations(dir: File): Annotations<NullabilityAnnotation> {
    return (inferAnnotations(
            apiJar,
            dir.annotationFiles(),
            inferrers,
            errorHandler = simpleErrorHandler { err, msg -> },
            loadOnly = true,
            propagationOverrides = hashMapOf(),
            existingAnnotations =
            hashMapOf(),
            packageIsInteresting = {true},
            existingPositionsToExclude = mapOf()
    ).groupByKey[NULLABILITY_KEY]!! as InferenceResultGroup<NullabilityAnnotation>).existingAnnotations
}
