package org.jetbrains.kannotator.plugin.actions

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.Task.Backgroundable
import java.io.File

data class InferringTaskParams(
        val inferNullabilityAnnotations: Boolean,
        val inferKotlinAnnotations: Boolean,
        val outputPath: String,
        val jarFiles: Collection<File>)

class InferringTask(project: Project, val taskParams: InferringTaskParams) : Backgroundable(project, "Infer Annotations", true) {
    override fun run(indicator: ProgressIndicator) {
        println(taskParams)
        Thread.sleep(10000);
    }

    override fun onCancel() {
        super<Backgroundable>.onCancel()
    }

    override fun onSuccess() {
        super<Backgroundable>.onSuccess()

    }
}