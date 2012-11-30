package org.jetbrains.annotator.actions

import com.intellij.openapi.roots.libraries.Library
import com.intellij.ui.CheckedTreeNode
import com.intellij.openapi.vfs.VirtualFile

public class LibraryCheckTreeNode(val library: Library): CheckedTreeNode(library)
public class JarFileCheckTreeNode(val jarFile: VirtualFile): CheckedTreeNode(jarFile)