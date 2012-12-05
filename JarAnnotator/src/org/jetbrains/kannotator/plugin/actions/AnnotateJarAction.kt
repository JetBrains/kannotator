package org.jetbrains.kannotator.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VfsUtilCore
import kotlinlib.toMap
import org.jetbrains.kannotator.plugin.actions.dialog.InferAnnotationDialog

public class AnnotateJarAction: AnAction() {
    public override fun actionPerformed(e: AnActionEvent?) {
        val project = e?.getProject()!!

        val dlg = InferAnnotationDialog(project)
        if (dlg.showAndGet()) {
            val params = InferringTaskParams(
                    inferNullabilityAnnotations = dlg.shouldInferNullabilityAnnotations(),
                    inferKotlinAnnotations = dlg.shouldInferKotlinAnnotations(),
                    outputPath = dlg.getConfiguredOutputPath(),
                    libJarFiles = dlg.getCheckedLibToJarFiles().map { it.key to it.value.map { file -> VfsUtilCore.virtualToIoFile(file) }.toSet() }.toMap()
            )

            ProgressManager.getInstance().run(InferringTask(project, params))
        }
    }
}