package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.ui.CheckboxTree

class LibraryCheckboxTree(val controller: LibraryItemsTreeController): CheckboxTree(LibraryCheckboxTreeRenderer(), controller.root)