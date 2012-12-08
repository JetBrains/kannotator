package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import java.util.ArrayList
import com.intellij.ui.CheckedTreeNode
import javax.swing.tree.TreeNode

class LibraryCheckboxTree(val controller: LibraryItemsTreeController): CheckboxTree(LibraryCheckboxTreeRenderer(), controller.root)

public fun CheckboxTreeBase.getCheckedNodesByNodeType<T: CheckedTreeNode>(nodeType : Class<T>) : Collection<T> {
    val nodes = ArrayList<T>()
    val root : Any = checkNotNull(getModel()?.getRoot(), "The root must no be null")

    if (root !is CheckedTreeNode) {
        throw IllegalStateException("The root must be instance of the " + javaClass<CheckedTreeNode>().getName() + ": " + root.javaClass.getName())
    }

    fun collect(node : CheckedTreeNode) {
        if (node.isLeaf()) {
            if (node.isChecked() && nodeType.isAssignableFrom(node.javaClass)) {
                nodes.add(node as T)
            }
        }
        else {
            for (i in 0..node.getChildCount() - 1) {
                val child : TreeNode? = node.getChildAt(i)
                if (child is CheckedTreeNode) {
                    collect(child as CheckedTreeNode)
                }
            }
        }
    }

    collect(root)

    return nodes
}
