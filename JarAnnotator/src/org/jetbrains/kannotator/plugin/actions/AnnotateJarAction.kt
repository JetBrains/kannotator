package org.jetbrains.kannotator.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.inferAnnotations
import org.jetbrains.kannotator.plugin.actions.dialog.InferAnnotationDialog
import org.objectweb.asm.ClassReader
import java.util.HashMap
import org.jetbrains.kannotator.main.NullabilityInferrer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.progress.ProgressManager

public class AnnotateJarAction: AnAction() {
    public override fun actionPerformed(e: AnActionEvent?) {
        val project = e?.getProject()!!

        val dlg = InferAnnotationDialog(project)
        if (dlg.showAndGet()) {
            val params = InferringTaskParams(
                    inferNullabilityAnnotations = dlg.shouldInferNullabilityAnnotations(),
                    inferKotlinAnnotations = dlg.shouldInferKotlinAnnotations(),
                    outputPath = dlg.getConfiguredOutputPath(),
                    jarFiles = dlg.getCheckedJarFiles().map { virtualFile -> VfsUtilCore.virtualToIoFile(virtualFile) }
            )

            ProgressManager.getInstance().run(InferringTask(project, params))
        }
    }
}