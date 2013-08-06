package org.jetbrains.kannotator.plugin.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.AnnotationOrderRootType
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToXMLByPackage
import org.jetbrains.kannotator.annotationsInference.nullability.NullabilityAnnotation
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.*
import org.jetbrains.kannotator.plugin.ideaUtils.runComputableInsideWriteAction
import org.jetbrains.kannotator.plugin.ideaUtils.runInsideReadAction
import org.jetbrains.kannotator.plugin.ideaUtils.runInsideWriteAction
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.controlFlow.builder.analysis.mutability.MutabilityAnnotation
import java.util.Collections
import com.intellij.openapi.progress.PerformInBackgroundOption
import org.jetbrains.kannotator.controlFlow.builder.analysis.MUTABILITY_KEY
import org.jetbrains.kannotator.controlFlow.builder.analysis.NULLABILITY_KEY
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.NO_ERROR_HANDLING
import org.jetbrains.kannotator.simpleErrorHandler

data class InferringTaskParams(
        val inferNullabilityAnnotations: Boolean,
        val inferKotlinAnnotations: Boolean,
        val addAnnotationsRoots: Boolean,
        val removeOtherRoots: Boolean,
        val outputPath: String,
        val useOneCommonTree: Boolean,
        val libJarFiles: Map<Library, Set<File>>)

public class InferringTask(val taskProject: Project, val taskParams: InferringTaskParams) :
        Backgroundable(taskProject, "Infer Annotations", true, PerformInBackgroundOption.DEAF) {
    private val INFERRING_RESULT_TAB_TITLE = "Annotate Jars"

    private var successMessage = "Success"

    public class InferringError(file: File, cause: Throwable?):
            Throwable("Exception during inferrence on file ${file.getName()}", cause)

    class InferringProgressIndicator(val indicator: ProgressIndicator, params: InferringTaskParams): ProgressMonitor() {
        val totalAmountOfJars: Int = params.libJarFiles.values().fold(0, { sum, files -> sum + files.size })
        var numberOfJarsFinished: Int = 0
        var numberOfMethods = 0
        var numberOfProcessedMethods = 0

        fun startJarProcessing(fileName: String, libraryName: String) {
            indicator.setText("Inferring for $fileName in $libraryName library. File: ${numberOfJarsFinished + 1} / $totalAmountOfJars.");
        }

        override fun processingStarted() {
            indicator.setText2("Initializing...");
            numberOfMethods = 0
            numberOfProcessedMethods = 0
        }

        override fun methodsProcessingStarted(methodCount: Int) {
            numberOfMethods = methodCount
            indicator.setText2("Inferring: 0%");
        }

        override fun processingComponentFinished(methods: Collection<Method>) {
            numberOfProcessedMethods += methods.size

            if (numberOfMethods != 0) {
                val progressPercent = (numberOfProcessedMethods.toDouble() / numberOfMethods * 100).toInt()
                indicator.setText2("Inferring: $progressPercent%");
            }
            else {
                indicator.setText2("Inferring: 100%");
            }
        }

        override fun processingFinished() {
            numberOfJarsFinished++
        }

        fun savingStarted() {
            indicator.setText2("Saving...")
        }

        fun savingFinished() {
            indicator.setText2("")
        }

        fun isCanceled() = indicator.isCanceled()
    }

    override fun run(indicator: ProgressIndicator) {
        val inferringProgressIndicator = InferringProgressIndicator(indicator, taskParams)

        val outputDirectory = checkNotNull(LocalFileSystem.getInstance()!!.refreshAndFindFileByPath(taskParams.outputPath),
                "Output folder ${taskParams.outputPath} is expected to be created before activating task")

        processFiles(outputDirectory, inferringProgressIndicator)

        if (!taskParams.libJarFiles.isEmpty()) {
            runInsideReadAction {
                if (taskParams.addAnnotationsRoots) {
                    outputDirectory.refresh(true, true, runnable {
                        runInsideWriteAction { ProjectRootManagerEx.getInstanceEx(getProject()!!)!!.makeRootsChange(EmptyRunnable.getInstance(), false, true) }
                    })
                }
                else {
                    outputDirectory.refresh(true, true)
                }
            }
        }
    }

    override fun onSuccess() {
        val project = getProject()!!

        if (project.isDisposed() || !project.isOpen()) {
            return
        }

        val numberOfFiles = taskParams.libJarFiles.values().fold(0, { sum, files -> sum + files.size })

        Notifications.Bus.notify(Notification(
                "KAnnotator", "Annotating finished successfully", "$numberOfFiles file(s) were annotated",
                NotificationType.INFORMATION), project)
    }

    public override fun onCancel() {
        val project = getProject()!!

        if (project.isDisposed() || !project.isOpen()) {
            return
        }

        Notifications.Bus.notify(
                Notification("KAnnotator", "Annotating was canceled", "Annotating was canceled", NotificationType.INFORMATION), project)
    }

    private fun processFiles(outputDirectory: VirtualFile, inferringProgressIndicator: InferringProgressIndicator) {
        for ((lib, files) in taskParams.libJarFiles) {

            val libOutputDir =
                    if (taskParams.useOneCommonTree)
                        outputDirectory
                    else
                        createOutputDirectory(lib, outputDirectory)

            val libIoOutputDir = VfsUtilCore.virtualToIoFile(libOutputDir)

            for (file in files) {
                inferringProgressIndicator.startJarProcessing(file.getName(), lib.getName() ?: "<no-name>")

                if (inferringProgressIndicator.isCanceled()) {
                    return
                }

                try {
                    val inferrerMap = HashMap<AnalysisType, AnnotationInferrer<Any, Qualifier>>()
                    if (taskParams.inferNullabilityAnnotations) {
                        inferrerMap[NULLABILITY_KEY] = NullabilityInferrer() as AnnotationInferrer<Any, Qualifier>
                    }
                    if (taskParams.inferKotlinAnnotations) {
                        inferrerMap[MUTABILITY_KEY] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any, Qualifier>
                    }

                    // TODO: Add existing annotations from dependent libraries
                    val inferenceResult = inferAnnotations(
                            FileBasedClassSource(arrayListOf(file)), ArrayList<File>(),
                            inferrerMap,
                            inferringProgressIndicator,
                            NO_ERROR_HANDLING,
                            false,
                            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
                            hashMapOf(NULLABILITY_KEY to AnnotationsImpl<NullabilityAnnotation>(), MUTABILITY_KEY to AnnotationsImpl<MutabilityAnnotation>()),
                            {true},
                            Collections.emptyMap()
                    )

                    inferringProgressIndicator.savingStarted()

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

                    writeAnnotationsToXMLByPackage(
                            declarationIndex,
                            declarationIndex,
                            null,
                            libIoOutputDir,
                            inferredNullabilityAnnotations,
                            propagatedNullabilityPositions,
                            simpleErrorHandler {
                                kind, message -> throw IllegalArgumentException(message)
                            })

                    inferringProgressIndicator.savingFinished()
                } catch (e: OutOfMemoryError) {
                    // Don't wrap OutOfMemoryError
                    throw e
                } catch (e: Throwable) {
                    throw InferringError(file, e)
                }
            }

            if (taskParams.addAnnotationsRoots) {
                assignAnnotationsToLibrary(lib, libOutputDir)
            }
        }
    }

    private fun createOutputDirectory(library: Library, outputDirectory: VirtualFile): VirtualFile {
        return runComputableInsideWriteAction {
            val libraryDirName = library.getName()?.replaceAll("[\\/:*?\"<>|]", "_") ?: "no-name"
            // Drop directory if it already exists.
            // We should not do that when flushing everything into the same directory tree, otherwise we can delete
            // something important left from previous libraries.
            if (!taskParams.useOneCommonTree) {
               outputDirectory.findChild(libraryDirName)?.delete(this@InferringTask)
            }

            outputDirectory.createChildDirectory(this@InferringTask, libraryDirName)
        }
    }

    private fun assignAnnotationsToLibrary(library: Library, annotationRootDir: VirtualFile) {
        runInsideWriteAction {
            val modifiableModel = library.getModifiableModel()
            try {
                if (taskParams.removeOtherRoots) {
                    for (annotationRoot in modifiableModel.getFiles(AnnotationOrderRootType.getInstance())) {
                        modifiableModel.removeRoot(annotationRoot.getUrl(), AnnotationOrderRootType.getInstance())
                    }
                }

                modifiableModel.addRoot(annotationRootDir, AnnotationOrderRootType.getInstance())
                modifiableModel.commit()
            }
            catch (error: Throwable) {
                Disposer.dispose(modifiableModel)
                throw error
            }
        }
    }
}