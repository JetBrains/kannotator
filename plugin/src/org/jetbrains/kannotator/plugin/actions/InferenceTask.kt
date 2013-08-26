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
import org.jetbrains.kannotator.annotations.io.writeAnnotationsToJaif
import org.jetbrains.kannotator.annotations.io.AnnotationsFormat
import org.jetbrains.kannotator.annotations.io.executeAnnotationTask
import org.jetbrains.kannotator.annotations.io.InferenceParams
import kotlinlib.mapKeysAndValues
import org.jetbrains.kannotator.annotations.io.InferenceException
import org.jetbrains.kannotator.plugin.actions.IdeaInferenceTask.InferenceProgressIndicator
import java.io.IOException
import java.io.FileNotFoundException
import org.jetbrains.kannotator.annotations.io.FileAwareProgressMonitor
import kotlinlib.prefixUpToLast
import org.jetbrains.kannotator.annotations.io.AnnotatedLibrary
import com.intellij.openapi.vfs.impl.http.LocalFileStorage
import com.intellij.openapi.vfs.VirtualFileManager


data class IdeaInferenceParams(
        val inference: InferenceParams,
        val annotatedToIdeaLibs: Map<AnnotatedLibrary, Library>,
        val addAnnotationsRoots: Boolean,
        val removeOtherRoots: Boolean
)


public class IdeaInferenceTask(val taskProject: Project,
                               val parameters: IdeaInferenceParams) :
    Backgroundable(taskProject, "Infer Annotations", true, PerformInBackgroundOption.DEAF) {

    inner class InferenceProgressIndicator(val indicator: ProgressIndicator) : FileAwareProgressMonitor() {
        val totalAmountOfJars: Int = parameters.annotatedToIdeaLibs.keySet().fold(0, { sum, annotatedLib -> sum + annotatedLib.files.size() })
        var numberOfJarsFinished: Int = 0
        var numberOfMethods = 0
        var numberOfProcessedMethods = 0

        override fun jarProcessingStarted(fileName: String, libraryName: String) {
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
            } else {
                indicator.setText2("Inferring: 100%");
            }
        }

        override fun jarProcessingFinished(fileName: String, libraryName: String) {
            numberOfJarsFinished++
            indicator.setText2("Saving...")
        }

        override fun processingAborted() {
            val project = getProject()!!
            if (!project.isDisposed() && project.isOpen()) {
                Notifications.Bus.notify(
                        Notification("KAnnotator", "Annotating was canceled", "Annotating was canceled", NotificationType.INFORMATION), project)
            }
        }
        fun isCanceled() = indicator.isCanceled()

        //All libraries are annotated
        override fun allFilesAreAnnotated() {
            val project = getProject()!!

            if (!project.isDisposed() && project.isOpen()) {
                val numberOfFiles = parameters.annotatedToIdeaLibs.keySet().fold(0, { sum, annotatedLib -> sum + annotatedLib.files.size() })
                val message = when(numberOfFiles) {
                    0 -> "No files were annotated"
                    1 -> "One file was annotated"
                    else -> "$numberOfFiles files were annotated"
                }
                Notifications.Bus.notify(Notification(
                        "KAnnotator", "Annotating finished successfully", message,
                        NotificationType.INFORMATION),
                        project)
            }
        }
    }

    var monitor: InferenceProgressIndicator? = null

    //Backgroundable implementation
    public override fun run(indicator: ProgressIndicator) {
        monitor = InferenceProgressIndicator(indicator)
        executeAnnotationTask(parameters.inference, monitor!!)
        assignAllAnnotations()
    }

    //Backgroundable implementation
    public override fun onCancel() {
        monitor!!.processingAborted()
    }

    private fun assignAllAnnotations() {
        for((annotatedLib, lib) in parameters.annotatedToIdeaLibs) {
            runInsideWriteAction {
                LocalFileSystem.getInstance()!!.refreshAndFindFileByPath(parameters.inference.outputPath)!!.refresh(false, true)
            }
            val path = annotatedLib.annotationsPath(parameters.inference.outputPath, parameters.inference.useOneCommonTree)
            val annotationRootDir = LocalFileSystem.getInstance()!!.findFileByPath(path)
            if (annotationRootDir != null) {
                assignAnnotationsToLibrary(lib, annotationRootDir, parameters.removeOtherRoots)
            } else {
                throw FileNotFoundException("Can't find virtual file for $path! Annotations for ${annotatedLib.name} won't be assigned")
            }
        }

        if (!parameters.annotatedToIdeaLibs.isEmpty()) {
            runInsideWriteAction {
                val outputDirectory = LocalFileSystem.getInstance()!!.refreshAndFindFileByPath(parameters.inference.outputPath)
                if (parameters.addAnnotationsRoots) {
                    outputDirectory?.refresh(true, true, runnable {
                        ProjectRootManagerEx.getInstanceEx(getProject()!!)!!.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                    })
                } else {
                    outputDirectory?.refresh(true, true)
                }
            }
        }
    }

    private fun assignAnnotationsToLibrary(library: Library, annotationRootDir: VirtualFile, removeOtherRoots: Boolean) {
        runInsideWriteAction {
            val modifiableModel = library.getModifiableModel()
            try {
                if (removeOtherRoots) {
                    modifiableModel.getFiles(AnnotationOrderRootType.getInstance())
                            .forEach { modifiableModel.removeRoot(it.getUrl(), AnnotationOrderRootType.getInstance()) }
                }

                modifiableModel.addRoot(annotationRootDir, AnnotationOrderRootType.getInstance())
                modifiableModel.commit()
            } catch (error: Throwable) {
                Disposer.dispose(modifiableModel)
                throw error
            }
        }
    }

}