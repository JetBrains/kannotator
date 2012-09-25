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

private class ClassHierarchyEdgeImpl(override val base: ClassNode, override val derived: ClassNode): ClassHierarchyEdge

private class ClassNodeImpl(override val name: ClassName): ClassNode {
    override val subClasses: MutableCollection<ClassHierarchyEdge> = ArrayList()
    override val superClasses: MutableCollection<ClassHierarchyEdge> = ArrayList()

    override val methods: MutableSet<Method> = HashSet()

    public fun toString(): String = name.canonical
}

public class ClassHierarchyGraphBuilder {
    private val classes: MutableCollection<ClassName> = hashSet()
    private val nodesByName: MutableMap<ClassName, ClassNodeImpl> = hashMap()

    private fun getNodeByName(name: ClassName) = nodesByName.getOrPut(name) { ClassNodeImpl(name) }

    private fun addEdge(base: ClassNodeImpl, derived: ClassNodeImpl) {
        val edge = ClassHierarchyEdgeImpl(base, derived)
        base.subClasses.add(edge)
        derived.superClasses.add(edge)
    }


    public fun addClass(name: ClassName) {
        classes.add(name)
    }

    public fun buildGraph(): ClassHierarchyGraph {
        for (name in classes) {
            val node = getNodeByName(name)
            val (methods, superClasses) = ClassHierarchyClassVisitor.process(name)
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
}

private data class MethodsAndSuperClasses(
        val methods: List<Method>,
        val superClasses: List<ClassName>
)

private class ClassHierarchyClassVisitor: ClassVisitor(Opcodes.ASM4) {
    val methods = arrayList<Method>()
    val superClasses = arrayList<ClassName>()

    var thisClassName: ClassName? = null

    val result: MethodsAndSuperClasses
        get() = MethodsAndSuperClasses(methods, superClasses)

    public override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        if (interfaces != null) {
            for (interface in interfaces) {
                superClasses.add(ClassName.fromInternalName(interface))
            }
        }
        if (superName != null) {
            superClasses.add(ClassName.fromInternalName(superName))
        }
        thisClassName = ClassName.fromInternalName(name)
    }

    public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val method = Method.create(thisClassName!!, name, desc)
        methods.add(method)
        return super.visitMethod(access, name, desc, signature, exceptions)
    }

    class object {
        fun process(val name: ClassName): MethodsAndSuperClasses {
            val visitor = ClassHierarchyClassVisitor()

            val stream = ClassLoader.getSystemResourceAsStream(name.internal + ".class")
            if (stream == null) {
                return visitor.result
            }

            val reader = ClassReader(stream)
            reader.accept(visitor, SKIP_CODE or SKIP_DEBUG or SKIP_FRAMES)

            return visitor.result
        }
    }
}
