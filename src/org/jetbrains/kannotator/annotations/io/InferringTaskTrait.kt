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
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.simpleErrorHandler
import kotlinlib.prefixUpToLast


public class InferringError(file: File, cause: Throwable?) : Throwable("Exception during inferrence on file ${file.getName()}", cause)

public open class AnnotationTaskProgressMonitor(): ProgressMonitor() {
    public open fun annotationTaskFinished() {}
    public open fun jarProcessingStarted(fileName: String, libraryName: String) {}
}
public open data class InferringParameters(
        public val inferNullabilityAnnotations: Boolean,
        public val mutability: Boolean,
        public val outputPath: String,
        public val useOneCommonTree: Boolean,
        public val libToFiles: Map<String, Set<File>>,
        public val outputFormat: AnnotationsFormat,
        public val verbose: Boolean = true)

public trait InferringTaskTrait<P : InferringParameters, M : AnnotationTaskProgressMonitor>
{
    public val parameters: P
    public var monitor: M?


    open fun beforeProcessing(params: InferringParameters) {}
    open fun afterProcessing(params: InferringParameters) {}
    open fun beforeFile(params: InferringParameters, filename: String, libname: String) {}
    open fun afterFile(params: InferringParameters, filename: String, libname: String) {}
    open fun beforeLibrary(params: InferringParameters, libname: String) {}
    open fun afterLibrary(params: InferringParameters, libname: String) {}

    public fun perform() {
        val outputDirectory = File(parameters.outputPath)
        outputDirectory.mkdir()

        beforeProcessing(parameters)
        processLibraries(outputDirectory, monitor!!)
        afterProcessing(parameters)
    }

    protected fun createOutputDirectory(library: String?, outputDirectory: File): File {
        val libraryDirName = (library?.prefixUpToLast('.') ?: library)?.replaceAll("[\\/:*?\"<>|]", "_") ?: "no-name"
        val libraryPath = outputDirectory.path + File.separator + libraryDirName
        // Drop directory if it already exists.
        // We should not do that when flushing everything into the same directory tree, otherwise we can delete
        // something important left from previous libraries.
        if (!parameters.useOneCommonTree) {
            outputDirectory.listFiles()?.find { file -> file.name == libraryDirName }?.delete()
        }
        val res = File(libraryPath)
        res.mkdir()
        return res
    }

    protected fun outputDirectoryForLibrary(lib: String, outputDirectory: File): File =
            if (parameters.useOneCommonTree)
                outputDirectory
            else
                createOutputDirectory(lib, outputDirectory)


    private fun buildInferrerMap(params: InferringParameters): HashMap<AnalysisType, AnnotationInferrer<Any, Qualifier>> {
        val inferrerMap = HashMap<AnalysisType, AnnotationInferrer<Any, Qualifier>>()
        if (parameters.inferNullabilityAnnotations) {
            inferrerMap[NULLABILITY_KEY] = NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>
        }
        if (parameters.mutability) {
            inferrerMap[MUTABILITY_KEY] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any, Qualifier>
        }
        return inferrerMap
    }

    protected fun processLibraries(outputDirectory: File, monitor: ProgressMonitor) {
        for ((lib, files) in parameters.libToFiles) {
            beforeLibrary(parameters, lib)
            val libOutputDir = outputDirectoryForLibrary(lib, outputDirectory)
            for (file in files) {
                try {
                    beforeFile(parameters, file.getName(), lib)
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

                    when (parameters.outputFormat) {
                        AnnotationsFormat.JAIF -> writeAnnotationsToJaif(
                                declarationIndex,
                                libOutputDir,
                                inferredNullabilityAnnotations,
                                propagatedNullabilityPositions)
                        AnnotationsFormat.XML -> writeAnnotationsToXMLByPackage(
                                declarationIndex,
                                declarationIndex,
                                null,
                                libOutputDir,
                                inferredNullabilityAnnotations,
                                propagatedNullabilityPositions,
                                simpleErrorHandler {
                                    kind, message ->
                                    throw IllegalArgumentException(message)
                                })
                        else -> throw UnsupportedOperationException(
                                "Given annotations output format is not supported")
                    }
                    afterFile(parameters, file.getName(), lib)
                } catch (e: OutOfMemoryError) {
                    // Don't wrap OutOfMemoryError
                    throw e
                } catch (e: InterruptedException) {
                    monitor.processingAborted()
                } catch (e: Throwable) {
                    throw InferringError(file, e)
                }
            }

            afterLibrary(parameters, lib)
        }
    }

}
