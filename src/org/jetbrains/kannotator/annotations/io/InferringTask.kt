package org.jetbrains.kannotator.annotations.io

import java.io.File

//

public class InferringError(file: File, cause: Throwable?) : Throwable("Exception during inferrence on file ${file.getName()}", cause)

class InferringTaskParams(
        public val inferNullabilityAnnotations: Boolean,
        public val mutability: Boolean,
        public val outputPath: String,
        public val useOneCommonTree: Boolean,
        public val libJarFiles: Map<String, Set<File>>,
        public val outputFormat: AnnotationsFormat,
        public val verbose: Boolean = true)

public trait InferringTaskTrait
{
   val parameters: InferringTaskParams

}


//package org.jetbrains.kannotator.client
//
//
//import org.jetbrains.kannotator.annotations.io.AnnotationsFormat
//import org.jetbrains.kannotator.declarations.Method
//import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif
//import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
//import org.jetbrains.kannotator.simpleErrorHandler
//import org.jetbrains.kannotator.index.DeclarationIndexImpl
//import java.util.Collections
//import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
//import org.jetbrains.kannotator.declarations.Annotations
//import org.jetbrains.kannotator.controlFlow.builder.analysis.*
//import org.jetbrains.kannotator.main.NullabilityInferrer
//import org.jetbrains.kannotator.main.AnnotationInferrer
//import java.util.HashMap
//import org.jetbrains.kannotator.runtime.annotations.AnalysisType
//import org.jetbrains.kannotator.main.inferAnnotations
//
//import java.io.File
//import java.util.ArrayList
//import java.util.HashMap
//import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
//import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
//import org.jetbrains.kannotator.index.FileBasedClassSource
//import org.jetbrains.kannotator.main.*
//import org.jetbrains.kannotator.plugin.ideaUtils.runComputableInsideWriteAction
//import org.jetbrains.kannotator.plugin.ideaUtils.runInsideReadAction
//import org.jetbrains.kannotator.plugin.ideaUtils.runInsideWriteAction
//import org.jetbrains.kannotator.declarations.*
//import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
//import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
//import java.util.Collections
//import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
//import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
//import org.jetbrains.kannotator.runtime.annotations.AnalysisType
//import org.jetbrains.kannotator.NO_ERROR_HANDLING
//import org.jetbrains.kannotator.simpleErrorHandler
//import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif
//import org.jetbrains.kannotator.annotations.io.AnnotationsFormat
//import java.io.PrintWriter
//import java.io.PrintStream
//
//public class SimpleInferringTask(val taskParams: InferringTaskParams, val diagnostics: PrintStream)  {
//
//

//    fun logAlways(string: String) = diagnostics.println(string)
//    fun logVerbose(string: String) = if (taskParams.verbose) diagnostics.println(string)
//    fun logVerboseNoNewline(string: String) = if (taskParams.verbose) diagnostics.print(string)
//
//    inner class TextInferringProgressIndicator(params: InferringTaskParams) : ProgressMonitor() {
//        val totalAmountOfJars: Int = params.libJarFiles.values().fold(0, { sum, files -> sum + files.size })
//        var numberOfJarsFinished: Int = 0
//        var numberOfMethods = 0
//        var numberOfProcessedMethods = 0
//
//        fun startJarProcessing(fileName: String, libraryName: String) {
//            logVerbose("Inferring for $fileName in $libraryName library. File: ${numberOfJarsFinished + 1} / $totalAmountOfJars.");
//        }
//
//        override fun processingStarted() {
//            logVerbose("Initializing...")
//        }
//
//        override fun methodsProcessingStarted(methodCount: Int) {
//            numberOfMethods = methodCount
//            //logVerbose("Inferring: 0%");
//        }
//
//        override fun processingComponentFinished(methods: Collection<Method>) {
//            numberOfProcessedMethods += methods.size
//
//            if (numberOfMethods != 0) {
//                val progressPercent = (numberOfProcessedMethods.toDouble() / numberOfMethods * 100).toInt()
//                logVerboseNoNewline("Inferring: $progressPercent% \r");
//            }
//            else {
//                logVerbose("Inferring: 100% \n");
//            }
//        }
//
//        override  fun processingFinished() {
//            numberOfJarsFinished++
//        }
//
//        fun savingStarted() {
//            logVerbose("Saving...")
//        }
//
//        fun savingFinished() {
//            val numberOfFiles = taskParams.libJarFiles.size
//            logVerbose("Annotating finished successfully: $numberOfFiles file(s) were annotated")
//        }
//
//        fun processingAborted() {
//            logVerbose("Annotating was canceled")
//        }
//    }
//
//    fun perform() {
//        val inferringProgressIndicator = TextInferringProgressIndicator(taskParams)
//        val outputDirectory = File(taskParams.outputPath)
//        outputDirectory.mkdir()
//        processFiles(outputDirectory, inferringProgressIndicator)
//    }
//
//
//    private fun processFiles(outputDirectory: File, inferringProgressIndicator: TextInferringProgressIndicator
//    ) {
//
//        for ((lib, files) in taskParams.libJarFiles) {
//
//            val libOutputDir =
//                    if (taskParams.useOneCommonTree)
//                        outputDirectory
//                    else
//                        createOutputDirectory(lib, outputDirectory)
//
//            for (file in files) {
//                inferringProgressIndicator.startJarProcessing(file.getName(), lib?: "<no-name>")
//
//
//
//                try {
//                    val inferrerMap = HashMap<AnalysisType, AnnotationInferrer<Any, Qualifier>>()
//                    if (taskParams.inferNullabilityAnnotations) {
//                        inferrerMap[NULLABILITY_KEY] = NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>
//                    }
//                    if (taskParams.mutability) {
//                        inferrerMap[MUTABILITY_KEY] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any, Qualifier>
//                    }
//
//                    // TODO: Add existing annotations from dependent libraries
//                    val inferenceResult = inferAnnotations(
//                            FileBasedClassSource(arrayListOf(file)), ArrayList<File>(),
//                            inferrerMap,
//                            inferringProgressIndicator,
//                            NO_ERROR_HANDLING,
//                            false,
//                            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
//                            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
//                            { true },
//                            Collections.emptyMap()
//                    )
//
//                    inferringProgressIndicator.savingStarted()
//
//                    val inferredNullabilityAnnotations =
//                            checkNotNull(
//                                    inferenceResult.groupByKey[NULLABILITY_KEY]!!.inferredAnnotations,
//                                    "Only nullability annotations are supported by now") as
//                            Annotations<NullabilityAnnotation>
//
//                    val propagatedNullabilityPositions =
//                            checkNotNull(
//                                    inferenceResult.groupByKey[NULLABILITY_KEY]!!.propagatedPositions,
//                                    "Only nullability annotations are supported by now"
//                            )
//
//                    val cs = FileBasedClassSource(arrayListOf(file))
//                    val declarationIndex = DeclarationIndexImpl(cs)
//
//                    when (taskParams.outputFormat) {
//                        AnnotationsFormat.JAIF -> writeAnnotationsToJaif(
//                                declarationIndex,
//                                libOutputDir,
//                                inferredNullabilityAnnotations,
//                                propagatedNullabilityPositions)
//                        AnnotationsFormat.XML -> writeAnnotationsToXMLByPackage(
//                                declarationIndex,
//                                declarationIndex,
//                                null,
//                                libOutputDir,
//                                inferredNullabilityAnnotations,
//                                propagatedNullabilityPositions,
//                                simpleErrorHandler {
//                                    kind, message ->
//                                    throw IllegalArgumentException(message)
//                                })
//                        else -> throw UnsupportedOperationException(
//                                "Given annotations output format is not supported")
//                    }
//                    inferringProgressIndicator.savingFinished()
//
//                } catch (e: OutOfMemoryError) {
//                    // Don't wrap OutOfMemoryError
//                    throw e
//                } catch (e: InterruptedException) {
//                    inferringProgressIndicator.processingAborted()
//                } catch (e: Throwable) {
//                    throw InferringError(file, e)
//                }
//            }
//
//        }
//    }
//
//    private fun createOutputDirectory(library: String?, outputDirectory: File): File {
//        val libraryDirName = library?.replaceAll("[\\/:*?\"<>|]", "_") ?: "no-name"
//        val libraryPath = outputDirectory.path + File.separator + libraryDirName
//        // Drop directory if it already exists.
//        // We should not do that when flushing everything into the same directory tree, otherwise we can delete
//        // something important left from previous libraries.
//        if (!taskParams.useOneCommonTree) {
//            outputDirectory.listFiles()?.find { file -> file.name == libraryDirName }?.delete()
//        }
//        val res = File(libraryPath)
//        res.mkdir()
//        return res
//    }
//
//}
