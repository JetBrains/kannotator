package org.jetbrains.kannotator.classHierarchy

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlinlib.*
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.MethodId
import org.jetbrains.kannotator.index.ClassSource
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

data class ClassData(val name: ClassName, methodsById: Map<MethodId, Method>) {
    val methodsById = methodsById
}

private class ClassNodeImpl(val name: ClassName): HierarchyNodeImpl<ClassData>() {
    private val methods = HashSet<Method>()

    private var _data: ClassData? = null
    override val data: ClassData
            get() {
                if (_data == null) {
                    _data = ClassData(name, methods.map { m -> m.id to m }.toMap())
                }
                return _data!!
            }

    fun addMethod(method: Method) {
        assert(_data == null) {"Trying to add method after initialization is finished"}
        methods.add(method)
    }

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
            node.addMethod(method)
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