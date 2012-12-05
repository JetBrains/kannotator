package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.util.ui.classpath.ChooseLibrariesFromTablesDialog
import com.intellij.util.ui.tree.TreeUtil
import java.util.HashMap
import java.util.LinkedHashSet
import javax.swing.tree.DefaultTreeModel
import kotlinlib.LazyValue

public class LibraryItemsTreeController() {
    private var initializedTree = LazyValue<CheckboxTree>()

    val root: CheckedTreeNode = CheckedTreeNode("libraries")

    public fun buildTree(initializedTreeView: CheckboxTree, project: Project) {
        root.removeAllChildren();
        initializedTree.set(initializedTreeView)

        val libraryTables = ChooseLibrariesFromTablesDialog.getLibraryTables(project, true)
        for (table in libraryTables) {
            for (library in table.getLibraries()) {
                val libraryNode = LibraryCheckTreeNode(library)
                libraryNode.setChecked(false)

                root.add(libraryNode)

                val classFileRoots = library.getRootProvider().getFiles(OrderRootType.CLASSES)
                for (classFileRoot in classFileRoots) {
                    if (classFileRoot.getExtension() == "jar") {
                        val jarFileCheckTreeNode = JarFileCheckTreeNode(classFileRoot)
                        jarFileCheckTreeNode.setChecked(false);

                        libraryNode.add(jarFileCheckTreeNode);
                    }
                }

                if (libraryNode.getChildCount() == 0) {
                    libraryNode.setEnabled(false)
                }
            }
        }

        (initializedTreeView.getModel() as DefaultTreeModel).nodeStructureChanged(root)
        TreeUtil.expandAll(initializedTreeView);
    }

    public fun getCheckedLibToJarFiles(): Map<Library, Set<VirtualFile>> {
        val checkedJarNodes = initializedTree.get().getCheckedNodesByNodeType(javaClass<JarFileCheckTreeNode>())

        val resultLibToJars = HashMap<Library, MutableSet<VirtualFile>>()

        for (jarNode in checkedJarNodes) {
            val libraryCheckTreeNode = jarNode.getParent()!! as LibraryCheckTreeNode
            resultLibToJars.getOrPut(libraryCheckTreeNode.library, { LinkedHashSet() }).add(jarNode.jarFile)
        }

        return resultLibToJars
    }
}
