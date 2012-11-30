package org.jetbrains.annotator.actions

import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import javax.swing.JTree
import com.intellij.util.PlatformIcons

class LibraryCheckboxTreeRenderer: CheckboxTreeCellRenderer() {
    override fun customizeRenderer(tree: JTree?, value: Any?,
            selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Unit {
        when (value) {
            is LibraryCheckTreeNode -> {
                getTextRenderer().setIcon(PlatformIcons.LIBRARY_ICON)
                getTextRenderer().append(value.library.getName() ?: "<no name>")
            }
            else -> { /* Do nothing */ }
        }
    }
}

