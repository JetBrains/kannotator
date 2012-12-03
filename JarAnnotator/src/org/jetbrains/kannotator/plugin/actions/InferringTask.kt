package org.jetbrains.kannotator.plugin.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
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
import kotlinlib.LazyValue
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
        val jarFiles: Collection<File>)

class InferringTask(project: Project, val taskParams: InferringTaskParams) : Backgroundable(project, "Infer Annotations", true) {
    private val INFERRING_RESULT_TAB_TITLE = "Annotate Jars"

    private var successMessage = "Success"
    private var inferException = LazyValue<Exception>()

    private val progressMonitor = object: ProgressMonitor() {
        // TODO: Notify about progress
    }

    override fun run(indicator: ProgressIndicator) {
        val inferrerMap = HashMap<String, AnnotationInferrer<Any>>()
        if (taskParams.inferNullabilityAnnotations) {
            inferrerMap["nullability"] = NullabilityInferrer() as AnnotationInferrer<Any>
        }
        if (taskParams.inferKotlinAnnotations) {
            inferrerMap["kotlin"] = MUTABILITY_INFERRER_OBJECT as AnnotationInferrer<Any>
        }

        try {
            // TODO: Add existing annotations
            inferAnnotations(FileBasedClassSource(taskParams.jarFiles), ArrayList<File>(), inferrerMap, progressMonitor, false)

            // TODO: Store collected annotations

            val fileNames = taskParams.jarFiles.map { it.getName() }
            successMessage = "Inferring was successfully fineshed for files: $fileNames."
        } catch (e: Exception) {
            inferException.set(e)
            // TODO: Rethrow and check it's in EA
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

        val text = if (!inferException.isInitialized()) {
            successMessage
        } else {
            "Exception was thrown during inferring\n${inferException.get()}"
        }

        val simpleToolWindowPanel = SimpleToolWindowPanel(false, true)
        simpleToolWindowPanel.add(PanelWithText(text))
        simpleToolWindowPanel.setToolbar(createToolbar().getComponent())

        return simpleToolWindowPanel
    }
}