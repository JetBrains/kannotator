package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method

trait ClassHierarchyGraph {
    val classes: Collection<ClassNode>
}

trait ClassHierarchyEdge {
    val base: ClassNode
    val derived: ClassNode
}

trait ClassNode {
    val subClasses: Collection<ClassHierarchyEdge>
    val superClasses: Collection<ClassHierarchyEdge>

    val name: ClassName

    val methods: Set<Method>
}
