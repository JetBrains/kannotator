package org.jetbrains.kannotator.plugin.actions

import com.intellij.openapi.project.Project
import com.intellij.util.ui.classpath.ChooseLibrariesFromTablesDialog
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.roots.libraries.Library
import com.intellij.psi.impl.JavaPsiFacadeEx
import com.intellij.openapi.vfs.JarFileSystem
import com.google.common.collect.HashMultimap
import com.intellij.openapi.roots.AnnotationOrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.projectRoots.Sdk
import kotlinlib.addNotNull
import com.intellij.openapi.roots.RootProvider
import java.util.HashSet

class JarIndex(val project: Project) {

    trait ClassPathInfo {
        val rootProvider : RootProvider
    }

    data class LibraryInfo(val lib: Library): ClassPathInfo {
        override val rootProvider: RootProvider = lib.getRootProvider()
        fun toString() = lib.toString()
    }

    data class SdkInfo(val sdk: Sdk): ClassPathInfo {
        override val rootProvider: RootProvider = sdk.getRootProvider()
        fun toString() = sdk.toString()
    }

    private val jarToLibrary = HashMultimap.create<VirtualFile, ClassPathInfo>();

    {
        val libraryTables = ChooseLibrariesFromTablesDialog.getLibraryTables(project, true)
        val libraries = libraryTables.flatMap { it.getLibraries() map { lib -> LibraryInfo(lib) } }

        val projectRootManager = ProjectRootManager.getInstance(project)
        val sdks = arrayListOf<Sdk>()
        sdks.addNotNull(projectRootManager.getProjectSdk())

        val moduleManager = ModuleManager.getInstance(project)
        for (module in moduleManager.getModules()) {
            sdks.addNotNull(ModuleRootManager.getInstance(module).getSdk())
        }

        val allLibInfos = HashSet(libraries + (sdks map {SdkInfo(it)}))

        for (libInfo in allLibInfos) {
            val jarFileRoots = libInfo.rootProvider.getFiles(OrderRootType.CLASSES)
                    .filter { it.getExtension() == "jar" }
                    .forEach {
                        rootFile ->
                        val jarFile = getContainingJar(rootFile)
                        jarToLibrary.put(if (jarFile != null) jarFile!! else rootFile, libInfo)
                    }
        }

    }

    fun getContainingJar(file: VirtualFile): VirtualFile? {
        return (file.getFileSystem() as? JarFileSystem)?.getVirtualFileForJar(file)
    }

    fun getContainingLibraries(jarFile: VirtualFile): Collection<ClassPathInfo> {
        return jarToLibrary[jarFile]
    }

    fun getAnnotationRoots(libraries: Collection<ClassPathInfo>): List<VirtualFile> {
        return libraries.flatMap { lib -> lib.rootProvider.getFiles(AnnotationOrderRootType.getInstance()).toList() }
    }
}