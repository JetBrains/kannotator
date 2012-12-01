package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.treeStructure.Tree.NodeFilter
import com.intellij.util.ui.classpath.ChooseLibrariesFromTablesDialog
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.tree.DefaultTreeModel
import kotlinlib.LazyValue

public class LibraryItemsTreeController() {
    private var initializedTree = LazyValue<CheckboxTree>()

    val root: CheckedTreeNode = CheckedTreeNode("libraries")

    public fun buildTree(initializedTreeView: CheckboxTree, project: Project) {
        root.removeAllChildren();
        initializedTree.value = initializedTreeView

        val libraryTables = ChooseLibrariesFromTablesDialog.getLibraryTables(project, true)
        for (table in libraryTables) {
            for (library in table.getLibraries()) {
                val libraryNode = LibraryCheckTreeNode(library)
                root.add(libraryNode)

                val classFileRoots = library.getRootProvider().getFiles(OrderRootType.CLASSES)
                for (classFileRoot in classFileRoots) {
                    if (classFileRoot.getExtension() == "jar") {
                        libraryNode.add(JarFileCheckTreeNode(classFileRoot));
                    }
                }

                if (libraryNode.getChildCount() == 0) {
                    libraryNode.setEnabled(false)
                    libraryNode.setChecked(false)
                }
            }
        }

        (initializedTreeView.getModel() as DefaultTreeModel).nodeStructureChanged(root)
        TreeUtil.expandAll(initializedTreeView);
    }

    public fun getCheckedJarFiles(): Collection<VirtualFile> {
        return initializedTree.value!!.getCheckedNodes(javaClass<VirtualFile>(), null as NodeFilter<VirtualFile>?).toList()
    }
}
