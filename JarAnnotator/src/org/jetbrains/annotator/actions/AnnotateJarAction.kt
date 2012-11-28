package org.jetbrains.annotator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.main.inferAnnotations
import org.objectweb.asm.ClassReader

public class AnnotateJarAction: AnAction() {
    public override fun actionPerformed(e: AnActionEvent?) {
        val dlg = InferAnnotationDialog(e?.getProject())
        if (dlg.showAndGet()) {
            val fakeClassSource = object : ClassSource {
                override fun forEach(body: (ClassReader) -> Unit) {
                    println("Hello from kannotator")
                }
            }

            inferAnnotations<String>(fakeClassSource, arrayList(), hashMap("test" to (org.jetbrains.kannotator.main.MUTABILITY_INFERRER as AnnotationInferrer<Any>)))
            return
        }
    }
}