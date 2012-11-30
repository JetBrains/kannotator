package org.jetbrains.annotator.actions

import com.intellij.openapi.roots.libraries.Library
import com.intellij.ui.CheckedTreeNode

public class LibraryCheckTreeNode(val library: Library): CheckedTreeNode(library)