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



public fun ClassNode.find(method: Method): Method? = methods.find { it.asmMethod == method.asmMethod }

public fun ClassNode.getOverriddenMethods(method: Method): Set<Method> {
    val my = find(method)
    if (my == null) return hashSet()

    val result = hashSet<Method>(my)

    if (method.asmMethod.getName() in hashSet("<init>", "<clinit>")) return result

    for (subClass in subClasses) {
        result.addAll(subClass.derived.getOverriddenMethods(method))
    }

    return result
}
