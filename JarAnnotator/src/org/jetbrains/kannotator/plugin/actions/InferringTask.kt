package org.jetbrains.kannotator.plugin.actions

import com.google.common.collect.Multimap
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.PanelWithText
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.MessageView
import com.intellij.util.ContentsUtil
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import javax.swing.JPanel
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.MUTABILITY_INFERRER_OBJECT
import org.jetbrains.kannotator.main.NullabilityInferrer
import org.jetbrains.kannotator.main.ProgressMonitor
import org.jetbrains.kannotator.main.inferAnnotations

data class InferringTaskParams(
        val inferNullabilityAnnotations: Boolean,
        val inferKotlinAnnotations: Boolean,
        val outputPath: String,
        val libJarFiles: Map<Library, Set<File>>)

public class InferringTask(project: Project, val taskParams: InferringTaskParams) : Backgroundable(project, "Infer Annotations", true) {
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

        override fun processingFinished(methods: Collection<Method>) {
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
    }

    override fun run(indicator: ProgressIndicator) {
        val inferringProgressIndicator = InferringProgressIndicator(indicator, taskParams)

        for ((lib, files) in taskParams.libJarFiles) {
            for (file in files) {
                inferringProgressIndicator.startJarProcessing(file.getName(), lib.getName() ?: "<no-name>")

                try {
                    val inferrerMap = HashMap<String, AnnotationInferrer<Any>>()
                    if (taskParams.inferNullabilityAnnotations) {
                        inferrerMap["nullability"] = NullabilityInferrer() as AnnotationInferrer<Any>
                    }
                    if (taskParams.inferKotlinAnnotations) {
                        inferrerMap["kotlin"] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any>
                    }

                    // TODO: Add existing annotations from dependent libraries
                    inferAnnotations(
                            FileBasedClassSource(arrayList(file)), ArrayList<File>(),
                            inferrerMap,
                            inferringProgressIndicator,
                            false)

                    // TODO: Store collected annotations
                } catch (e: Throwable) {
                    throw InferringError(file, e)
                }
            }
        }
    }

    override fun onSuccess() {
        val project = getProject()!!

        if (project.isDisposed() || !project.isOpen()) {
            return
        }

        val messageView = MessageView.SERVICE.getInstance(myProject)!!
        val messageViewContentManager = messageView.getContentManager()!!
        val contentFactory = ContentFactory.SERVICE.getInstance()!!
        val toolWindowManager = ToolWindowManager.getInstance(project)!!

        val content = contentFactory.createContent(createMessageOutput(), INFERRING_RESULT_TAB_TITLE, true)

        ContentsUtil.addOrReplaceContent(messageViewContentManager, content, true)

        toolWindowManager.getToolWindow(ToolWindowId.MESSAGES_WINDOW)!!.activate(null)
    }

    private fun createMessageOutput() : JPanel {
        fun createToolbar() : ActionToolbar {
            val group = DefaultActionGroup()
            // TODO: Add close and pin action to toolbar
            return ActionManager.getInstance()!!.createActionToolbar(ActionPlaces.UNKNOWN, group, false)
        }

        val simpleToolWindowPanel = SimpleToolWindowPanel(false, true)
        simpleToolWindowPanel.add(PanelWithText(successMessage))
        simpleToolWindowPanel.setToolbar(createToolbar().getComponent())

        return simpleToolWindowPanel
    }
}