package org.jetbrains.kannotator.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.kannotator.plugin.actions.dialog.InferAnnotationDialog
import org.jetbrains.kannotator.annotations.io.AnnotationsFormat
import org.jetbrains.kannotator.annotations.io.InferenceParams
import org.jetbrains.kannotator.annotations.io.AnnotatedLibrary
import org.jetbrains.kannotator.plugin.ideaUtils.toIO

public class AnnotateJarAction : AnAction() {
    public override fun actionPerformed(e: AnActionEvent?) {
        annotateJars(e?.project!!)
    }

    public fun annotateJars(project: Project) {
        val dlg = InferAnnotationDialog(project)
        if (dlg.showAndGet()) {
            val libToAnnotatedLib = dlg.checkedLibToJarFiles
                    .map { AnnotatedLibrary(it.key.name!!, it.value.toIO()) to it.key }.toMap()

            val params = IdeaInferenceParams(
                    InferenceParams(
                            inferNullabilityAnnotations = dlg.shouldInferNullabilityAnnotations(),
                            mutability = dlg.shouldInferKotlinAnnotations(),
                            outputPath = dlg.configuredOutputPath,
                            libraries = libToAnnotatedLib.keySet(),
                            useOneCommonTree = dlg.useOneCommonTree(),
                            outputFormat = AnnotationsFormat.XML,
                            verbose = false),
                    libToAnnotatedLib,
                    removeOtherRoots = dlg.shouldRemoveAllOtherRoots(),
                    addAnnotationsRoots = dlg.shouldAddAnnotationsRoots()
            )

            ProgressManager.getInstance().run(IdeaInferenceTask(project, params))
        }
    }
}