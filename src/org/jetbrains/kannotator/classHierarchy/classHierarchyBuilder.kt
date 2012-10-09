package org.jetbrains.kannotator.classHierarchy

import java.util.HashMap
import kotlinlib.*
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.ClassSource
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.HashSet
import java.util.ArrayList
import org.jetbrains.kannotator.declarations.MethodId

data class ClassData(val name: ClassName, methods: Collection<Method>) {
    val methodsById: Map<MethodId, Method> = methods.map {m -> Pair(m.id, m)}.toMap()
}

private class ClassNodeImpl(val name: ClassName): HierarchyNodeImpl<ClassData>() {
    val methods: MutableSet<Method> = HashSet()

    override val data: ClassData = ClassData(name, methods)

    public fun toString(): String = name.internal
}

val HierarchyNode<ClassData>.methods: Collection<Method>
    get() = data.methodsById.values()

val HierarchyNode<ClassData>.name: ClassName
    get() = data.name


fun buildClassHierarchyGraph(classSource: ClassSource): HierarchyGraph<ClassData> {
    val nodesByName = HashMap<ClassName, ClassNodeImpl>()

    fun getNodeByName(name: ClassName) = nodesByName.getOrPut(name) { ClassNodeImpl(name) }

    fun addEdge(base: ClassNodeImpl, derived: ClassNodeImpl) {
        val edge = HierarchyEdgeImpl(base, derived)
        base.addChild(edge)
        derived.addParent(edge)
    }

    classSource forEach {
        reader ->
        val className = ClassName.fromInternalName(reader.getClassName())
        val node = getNodeByName(className)
        val (methods, superClasses) = processClass(reader)
        for (superClass in superClasses) {
            val superClassNode = getNodeByName(superClass)
            addEdge(base = superClassNode, derived = node)
        }
        for (method in methods) {
            node.methods.add(method)
        }
    }

    return HierarchyGraphImpl(nodesByName.values())
}

private data class MethodsAndSuperClasses(
        val methods: List<Method>,
        val superClasses: List<ClassName>
)

private fun processClass(reader: ClassReader): MethodsAndSuperClasses {
    val thisClassName = ClassName.fromInternalName(reader.getClassName())

    val methods = ArrayList<Method>()
    val superClasses = ArrayList<ClassName>()

    reader.accept(
            object : ClassVisitor(Opcodes.ASM4) {

                public override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                    for (interface in interfaces.orEmptyArray()) {
                        superClasses.add(ClassName.fromInternalName(interface))
                    }
                    if (superName != null) {
                        superClasses.add(ClassName.fromInternalName(superName))
                    }
                }

                public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    val method = Method(thisClassName, access, name, desc)
                    methods.add(method)
                    return null
                }
            },
            flags(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES)
    )

    return MethodsAndSuperClasses(methods, superClasses)
}