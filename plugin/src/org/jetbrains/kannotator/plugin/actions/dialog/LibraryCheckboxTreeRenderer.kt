package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.ui.CheckboxTree.CheckboxTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.PlatformIcons
import javax.swing.JTree

class LibraryCheckboxTreeRenderer: CheckboxTreeCellRenderer() {
    override fun customizeRenderer(tree: JTree?, value: Any?,
            selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Unit {
        when (value) {
            is LibraryCheckTreeNode -> {
                textRenderer.icon = PlatformIcons.LIBRARY_ICON
                textRenderer.append(value.library.name ?: "<no name>")
            }
            is JarFileCheckTreeNode -> {
                textRenderer.icon = value.jarFile.fileType.icon
                textRenderer.append(value.jarFile.name)
                textRenderer.append(" (${value.jarFile.path})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
            else -> { /* Do nothing */ }
        }
    }
}

