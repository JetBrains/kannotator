package org.jetbrains.kannotator.classHierarchy

import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method

public trait ClassHierarchyGraph {
    val classes: Collection<ClassNode>
}

public trait ClassHierarchyEdge {
    val base: ClassNode
    val derived: ClassNode
}

public trait ClassNode {
    val subClasses: Collection<ClassHierarchyEdge>
    val superClasses: Collection<ClassHierarchyEdge>

    val name: ClassName

    val methods: Set<Method>
}


