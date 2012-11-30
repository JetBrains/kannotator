package org.jetbrains.annotator.actions

import com.intellij.ui.CheckedTreeNode
import javax.swing.JTree
import com.intellij.ide.util.treeView.TreeState
import com.intellij.openapi.roots.OrderRootType
import javax.swing.tree.DefaultTreeModel
import com.intellij.openapi.project.Project
import com.intellij.util.ui.classpath.ChooseLibrariesFromTablesDialog

public class LibraryItemsTreeController() {
    public val root: CheckedTreeNode = CheckedTreeNode("libraries")

    public fun buildTree(initializedTreeView: JTree, project: Project) {
        root.removeAllChildren();

        val libraryTables = ChooseLibrariesFromTablesDialog.getLibraryTables(project, true)
        for (table in libraryTables) {
            for (library in table.getLibraries()) {
                val libraryNode = LibraryCheckTreeNode(library)
                root.add(libraryNode);

                val classFileRoots = library.getRootProvider().getFiles(OrderRootType.CLASSES)
                for (classFileRoot in classFileRoots) {
                    if (classFileRoot.getExtension() == "jar") {
                        libraryNode.add(JarFileCheckTreeNode(classFileRoot));
                    }
                }
            }
        }

        initializedTreeView.setShowsRootHandles(true)
        (initializedTreeView.getModel() as DefaultTreeModel).nodeStructureChanged(root)
    }
}
