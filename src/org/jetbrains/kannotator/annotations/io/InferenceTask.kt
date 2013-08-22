package org.jetbrains.kannotator.annotations.io

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
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.simpleErrorHandler
import kotlinlib.prefixUpToLast
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.ErrorHandler

public class InferenceException(file: File, cause: Throwable?) : Throwable("Exception during inferrence on file ${file.getName()}", cause)

public open class FileAwareProgressMonitor() : ProgressMonitor() {
    public open fun allFilesAreAnnotated() {
    }
    public open fun jarProcessingStarted(fileName: String, libraryName: String) {
    }
    public open fun jarProcessingFinished(fileName: String, libraryName: String) {
    }
    public open fun processingAborted() {}
}

public data class AnnotatedLibrary(
        public val name: String,
        public val files: Set<File>){

    public val refinedFileName: String
        get() = (name.prefixUpToLast(".jar") ?: name).replaceAll("[\\/:*?\"<>|]", "_")

    public fun annotationsPath(outputPath: String,
                               useOneCommonTree: Boolean): String =
            if (useOneCommonTree)
                outputPath
            else
                outputPath + File.separator + refinedFileName


}

public open data class InferenceParams(
        public val inferNullabilityAnnotations: Boolean,
        public val mutability: Boolean,
        public val outputPath: String,
        public val useOneCommonTree: Boolean,
        public val libraries: Iterable<AnnotatedLibrary>,
        public val outputFormat: AnnotationsFormat,
        public val verbose: Boolean = true)


public fun executeAnnotationTask(parameters: InferenceParams,
                                 monitor: FileAwareProgressMonitor) {

    createDirectory(parameters.outputPath)

    for (lib in parameters.libraries) {
        val outputDirectoryPath = lib.annotationsPath(parameters.outputPath, parameters.useOneCommonTree)
        val outputDirectory = prepareDirectoryForAnnotations(outputDirectoryPath, parameters.useOneCommonTree)
        for (file in lib.files) {
            try {
                monitor.jarProcessingStarted(file.getName(), lib.name)
                processFile(file, lib, parameters, monitor, outputDirectory)
                monitor.jarProcessingFinished(file.getName(), lib.name)
            } catch (e: OutOfMemoryError) {
                // Don't wrap OutOfMemoryError
                throw e
            } catch (e: InterruptedException) {
                monitor.processingAborted()
            } catch (e: Throwable) {
                throw InferenceException(file, e)
            }
        }
    }
    monitor.allFilesAreAnnotated()
}

private fun processFile(file: File,
                        lib: AnnotatedLibrary,
                        parameters: InferenceParams,
                        monitor: FileAwareProgressMonitor,
                        outputDirectory: File) {
    // TODO: Add existing annotations from dependent libraries
    val inferenceResult = inferAnnotations(
            FileBasedClassSource(arrayListOf(file)), ArrayList<File>(),
            buildInferrerMap(parameters),
            monitor,
            NO_ERROR_HANDLING,
            false,
            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
            { true },
            Collections.emptyMap()
    )

    val inferredNullabilityAnnotations =
            checkNotNull(
                    inferenceResult.groupByKey[NULLABILITY_KEY]!!.inferredAnnotations,
                    "Only nullability annotations are supported by now") as
            Annotations<NullabilityAnnotation>
    val propagatedNullabilityPositions =
            checkNotNull(
                    inferenceResult.groupByKey[NULLABILITY_KEY]!!.propagatedPositions,
                    "Only nullability annotations are supported by now"
            )

    val declarationIndex = DeclarationIndexImpl(FileBasedClassSource(arrayListOf(file)))

    writeAnnotations(parameters.outputFormat,
            declarationIndex,
            outputDirectory,
            lib.refinedFileName,
            inferredNullabilityAnnotations,
            propagatedNullabilityPositions)
}

private fun createDirectory(path: String): File {
    val dir = File(path)
    dir.mkdir()
    return dir
}

private fun buildInferrerMap(parameters: InferenceParams): Map<AnalysisType, AnnotationInferrer<Any, Qualifier>> {
    val inferrerMap = HashMap<AnalysisType, AnnotationInferrer<Any, Qualifier>>()
    if (parameters.inferNullabilityAnnotations) {
        inferrerMap[NULLABILITY_KEY] = NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>
    }
    if (parameters.mutability) {
        inferrerMap[MUTABILITY_KEY] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any, Qualifier>
    }
    return inferrerMap
}


private fun prepareDirectoryForAnnotations(path: String,
                                           useOneCommonTree: Boolean): File {
    val dir = createDirectory(path)
    if (!useOneCommonTree){
        dir.listFiles()?.forEach { it.delete() }
    }
    return dir
}

private fun writeAnnotations(
        outputFormat: AnnotationsFormat,
        declarationIndex: DeclarationIndexImpl,
        outputDirectory: File,
        libraryName: String,
        inferredNullabilityAnnotations: Annotations<NullabilityAnnotation>,
        propagatedNullabilityPositions: Set<AnnotationPosition>
) {
    when (outputFormat) {
        AnnotationsFormat.JAIF -> writeAnnotationsToJaif(
                declarationIndex,
                outputDirectory,
                libraryName,
                inferredNullabilityAnnotations,
                propagatedNullabilityPositions)

        AnnotationsFormat.XML -> writeAnnotationsToXMLByPackage(
                declarationIndex,
                declarationIndex,
                null,
                outputDirectory,
                inferredNullabilityAnnotations,
                propagatedNullabilityPositions,
                simpleErrorHandler {
                    kind, message ->
                    throw IllegalArgumentException(message)
                })
        else -> throw UnsupportedOperationException(
                "Given annotations output format is not supported")
    }

}
