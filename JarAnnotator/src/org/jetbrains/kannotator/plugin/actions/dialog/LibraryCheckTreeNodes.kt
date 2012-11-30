package org.jetbrains.kannotator.plugin.actions.dialog

import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckedTreeNode

public class LibraryCheckTreeNode(val library: Library): CheckedTreeNode(library)
public class JarFileCheckTreeNode(val jarFile: VirtualFile): CheckedTreeNode(jarFile)