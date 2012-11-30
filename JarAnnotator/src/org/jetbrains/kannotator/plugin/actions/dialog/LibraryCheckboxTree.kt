package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode

trait LibraryCheckboxTreeDelegate {
    open fun didSelectNode(node: CheckedTreeNode?): Unit
    open fun nodeStateChanged(node: CheckedTreeNode?): Unit
}

public class LibraryCheckboxTree(controller: LibraryItemsTreeController): CheckboxTree(LibraryCheckboxTreeRenderer(), controller.root)  {
    protected override fun onDoubleClick(node: CheckedTreeNode?): Unit {
        super.onDoubleClick(node)
        delegate?.didSelectNode(node)
    }

    protected override fun onNodeStateChanged(node: CheckedTreeNode?): Unit {
        super.onNodeStateChanged(node)
        delegate?.nodeStateChanged(node)
    }

    public var delegate: LibraryCheckboxTreeDelegate? = null
}