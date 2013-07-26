package org.jetbrains.kannotator.plugin.actions

import java.util.HashSet
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kannotator.runtime.annotations.AnalysisType
import org.jetbrains.kannotator.main.AnnotationInferrer
import org.jetbrains.kannotator.controlFlow.builder.analysis.Qualifier
import org.jetbrains.kannotator.declarations.MutableAnnotations
import org.jetbrains.kannotator.declarations.ClassMember
import org.jetbrains.kannotator.declarations.canonicalName
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.impl.JavaPsiFacadeEx
import java.util.LinkedHashSet
import org.jetbrains.kannotator.index.AnnotationKeyIndex
import org.objectweb.asm.ClassReader
import java.io.FileReader
import java.io.InputStreamReader
import java.io.Reader
import org.jetbrains.kannotator.index.FileBasedClassSource
import java.io.File
import org.jetbrains.kannotator.main.loadExternalAnnotations
import java.util.ArrayList
import com.intellij.openapi.vfs.VfsUtil

class DependencyManager(private val project: Project) {
    private val jarIndex = JarIndex(project)

    private val addedAnnotationRoots = HashSet<VirtualFile>()
    private val addedClasses = HashSet<VirtualFile>()
    private val addedJars = HashSet<VirtualFile>()

    public fun loadAnnotationsForDependencies(
            member: ClassMember,
            declarationIndex: DeclarationIndexImpl,
            onLoad: (Collection<() -> Reader>) -> Unit
    ) {
        ApplicationManager.getApplication().runReadAction(Computable {
            val canonicalName = member.declaringClass.canonicalName
            val facade = JavaPsiFacadeEx.getInstanceEx(project)
            val psiClass = facade.findClass(canonicalName)
            if (psiClass != null) {
                val virtualFile = psiClass.getContainingFile()?.getVirtualFile()
                if (virtualFile != null) {
                    if (addedClasses.add(virtualFile)) {
                        declarationIndex.addClass(ClassReader(virtualFile.getInputStream()?.readBytes()))

                        val jar = jarIndex.getContainingJar(virtualFile)
                        if (jar != null) {
                            if (addedJars.add(jar)) {
                                val libraries = jarIndex.getContainingLibraries(jar)
                                val annotationRoots = jarIndex.getAnnotationRoots(libraries)

                                for (root in annotationRoots) {
                                    if (!addedAnnotationRoots.add(root)) continue

                                    onLoad(loadAnnotationDataFromRoot(root))
                                }
                            }
                        }
                    }
                }
            }
        })

    }

    private fun loadAnnotationDataFromRoot(root: VirtualFile): Collection<() -> Reader> {
        val result = ArrayList<() -> Reader>()
        VfsUtil.processFilesRecursively(root) {
            file ->
            if (file!!.getExtension() == "xml") {
                result.add { InputStreamReader(file.getInputStream()!!) }
            }
            true
        }
        return result
    }

}