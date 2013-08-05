package org.jetbrains.kannotator.plugin.actions

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import kotlinlib.toMap
import org.jetbrains.kannotator.plugin.actions.dialog.InferAnnotationDialog

public class AnnotateJarAction: AnAction() {
    public override fun actionPerformed(e: AnActionEvent?) {
        annotateJars(e?.getProject()!!)
    }

    public fun annotateJars(project: Project) {
        val dlg = InferAnnotationDialog(project)
        if (dlg.showAndGet()) {
            val params = InferringTaskParams(
                    inferNullabilityAnnotations = dlg.shouldInferNullabilityAnnotations(),
                    inferKotlinAnnotations = dlg.shouldInferKotlinAnnotations(),
                    outputPath = dlg.getConfiguredOutputPath(),
                    libJarFiles = dlg.getCheckedLibToJarFiles().map { it.key to it.value.map { file -> VfsUtilCore.virtualToIoFile(file) }.toSet() }.toMap(),
                    addAnnotationsRoots = dlg.shouldAddAnnotationsRoots(),
                    useOneCommonTree = dlg.useOneCommonTree(),
                    removeOtherRoots = dlg.shouldRemoveAllOtherRoots()
            )

            ProgressManager.getInstance().run(InferringTask(project, params))
        }
    }
}