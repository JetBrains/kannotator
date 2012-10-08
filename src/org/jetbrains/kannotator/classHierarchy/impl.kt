package org.jetbrains.kannotator.classHierarchy

import java.util.ArrayList
import java.util.HashSet
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.HashMap
import kotlinlib.flags

private class ClassHierarchyEdgeImpl(override val base: ClassNode, override val derived: ClassNode): ClassHierarchyEdge

private class ClassNodeImpl(override val name: ClassName): ClassNode {
    override val subClasses: MutableCollection<ClassHierarchyEdge> = ArrayList()
    override val superClasses: MutableCollection<ClassHierarchyEdge> = ArrayList()

    override val methods: MutableSet<Method> = HashSet()

    public fun toString(): String = name.internal
}

fun buildClassHierarchyGraph(classes: Collection<ClassName>): ClassHierarchyGraph {
    val nodesByName = HashMap<ClassName, ClassNodeImpl>()

    fun getNodeByName(name: ClassName) = nodesByName.getOrPut(name) { ClassNodeImpl(name) }

    fun addEdge(base: ClassNodeImpl, derived: ClassNodeImpl) {
        val edge = ClassHierarchyEdgeImpl(base, derived)
        base.subClasses.add(edge)
        derived.superClasses.add(edge)
    }

    for (name in classes) {
        val node = getNodeByName(name)
        val (methods, superClasses) = processClass(name)
        for (superClass in superClasses) {
            val superClassNode = getNodeByName(superClass)
            addEdge(base = superClassNode, derived = node)
        }
        for (methodName in methods) {
            node.methods.add(methodName)
        }
    }

    return object : ClassHierarchyGraph {
        override val classes: Collection<ClassNode> = nodesByName.values()
    }
}

private data class MethodsAndSuperClasses(
        val methods: List<Method>,
        val superClasses: List<ClassName>
)

private fun processClass(name: ClassName): MethodsAndSuperClasses {
    val methods = arrayList<Method>()
    val superClasses = arrayList<ClassName>()

    val stream = ClassLoader.getSystemResourceAsStream(name.internal + ".class")
    if (stream == null) {
        return MethodsAndSuperClasses(methods, superClasses)
    }

    val reader = ClassReader(stream)
    val thisClassName = ClassName.fromInternalName(reader.getClassName())

    reader.accept(object : ClassVisitor(Opcodes.ASM4) {

                public override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
                    if (interfaces != null) {
                        for (interface in interfaces) {
                            superClasses.add(ClassName.fromInternalName(interface))
                        }
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
            flags(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES))

    return MethodsAndSuperClasses(methods, superClasses)
}