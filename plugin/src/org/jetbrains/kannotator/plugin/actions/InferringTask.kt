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
import org.jetbrains.kannotator.annotations.io.InferringTaskTrait
import org.jetbrains.kannotator.annotations.io.InferringParameters
import kotlinlib.mapKeysAndValues
import org.jetbrains.kannotator.annotations.io.InferringError
import org.jetbrains.kannotator.plugin.actions.PluginInferringTask.InferringProgressIndicator
import java.io.IOException
import java.io.FileNotFoundException
import org.jetbrains.kannotator.annotations.io.AnnotationTaskProgressMonitor


class InferringPluginParams(
        inferNullabilityAnnotations: Boolean,
        inferKotlinAnnotations: Boolean,
        val addAnnotationsRoots: Boolean,
        val removeOtherRoots: Boolean,
        outputPath: String,
        useOneCommonTree: Boolean,
        val libJarFiles: Map<Library, Set<File>>,
        outputFormat: AnnotationsFormat) : InferringParameters(
        inferNullabilityAnnotations,
        inferKotlinAnnotations,
        outputPath,
        useOneCommonTree,
        libJarFiles.mapKeysAndValues({(k, v)-> k.getName()!! }, { k, v-> v }),
        outputFormat,
        true
)

public class PluginInferringTask(val taskProject: Project, taskParams: InferringPluginParams) :
    Backgroundable(taskProject, "Infer Annotations", true, PerformInBackgroundOption.DEAF),
    InferringTaskTrait<InferringPluginParams, InferringProgressIndicator>
{
    public override var monitor: InferringProgressIndicator? = null
    public override val parameters: InferringPluginParams = taskParams

    inner class InferringProgressIndicator(val indicator: ProgressIndicator, params: InferringPluginParams) : AnnotationTaskProgressMonitor() {
        val totalAmountOfJars: Int = params.libJarFiles.values().fold(0, { sum, files -> sum + files.size })
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
            }
            else {
                indicator.setText2("Inferring: 100%");
            }
        }

        //processing of a single jar is finished
        override fun processingFinished() {
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
        override fun annotationTaskFinished(){
            val project = getProject()!!

            if (!project.isDisposed() && project.isOpen()) {
                val numberOfFiles = parameters.libJarFiles.values().fold(0, { sum, files -> sum + files.size })
                val message = when(numberOfFiles)
                {
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

    private fun assignAnnotationsToLibrary(library: Library, annotationRootDir: VirtualFile) {
        runInsideWriteAction {
            val modifiableModel = library.getModifiableModel()
            try {
                if (parameters.removeOtherRoots) {
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

    //Backgroundable implementation
    public override fun run(indicator: ProgressIndicator) {
        monitor = InferringProgressIndicator(indicator, parameters)
        perform()
    }

    //Backgroundable implementation
    override fun onSuccess() {
        monitor!!.annotationTaskFinished()
    }

    //Backgroundable implementation
    public override fun onCancel() {
        monitor!!.processingAborted()
    }


    override fun beforeFile(params: InferringParameters, filename: String, libname: String) {
        monitor!!.jarProcessingStarted(filename, libname)
    }

    override fun afterProcessing(params: InferringParameters) {
        val outputDirectory = checkNotNull(
                LocalFileSystem.getInstance()!!.refreshAndFindFileByPath(parameters.outputPath),
                "Output folder ${parameters.outputPath} is expected to be created before activating task")
        val outputDirectoryIO = VfsUtilCore.virtualToIoFile(outputDirectory)

        if (!parameters.libJarFiles.isEmpty()) {
            runInsideReadAction {
                if (parameters.addAnnotationsRoots) {
                    outputDirectory.refresh(true, true, runnable {
                        runInsideWriteAction { ProjectRootManagerEx.getInstanceEx(getProject()!!)!!.makeRootsChange(EmptyRunnable.getInstance(), false, true) }
                    })
                }
                else {
                    outputDirectory.refresh(true, true)
                }
            }
        }
        //add annotation roots for all libraries
        if (parameters.addAnnotationsRoots){
            for ((lib, files) in parameters.libJarFiles){
                val virtFile = LocalFileSystem.getInstance()!!.refreshAndFindFileByIoFile(
                        outputDirectoryForLibrary(lib.getName()!!, outputDirectoryIO))
                if(virtFile != null)
                    assignAnnotationsToLibrary(lib, virtFile)
                else
                    throw FileNotFoundException("virtual file for library ${lib.getName()} is not found!")
            }
        }
    }
}