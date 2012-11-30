package org.jetbrains.annotator.actions

import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import javax.swing.JTree
import com.intellij.util.PlatformIcons
import com.intellij.ui.SimpleTextAttributes

class LibraryCheckboxTreeRenderer: CheckboxTreeCellRenderer() {
    override fun customizeRenderer(tree: JTree?, value: Any?,
            selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Unit {
        when (value) {
            is LibraryCheckTreeNode -> {
                getTextRenderer().setIcon(PlatformIcons.LIBRARY_ICON)
                getTextRenderer().append(value.library.getName() ?: "<no name>")

                if (value.getChildCount() == 0) {
                    value.setEnabled(false)
                }
            }
            is JarFileCheckTreeNode -> {
                getTextRenderer().setIcon(value.jarFile.getFileType().getIcon())
                getTextRenderer().append(value.jarFile.getName())
                getTextRenderer().append(" (${value.jarFile.getPath()})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            else -> { /* Do nothing */ }
        }
    }
}

