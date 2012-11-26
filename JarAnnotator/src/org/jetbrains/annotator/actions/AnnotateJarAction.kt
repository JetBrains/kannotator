package org.jetbrains.annotator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Log
import org.jetbrains.kannotator.main.NullabilityInferrer

public class AnnotateJarAction: AnAction() {
    public override fun actionPerformed(e: AnActionEvent?) {
        val inferrer = NullabilityInferrer()
        Log.print("Annotating jar $inferrer")
    }
}