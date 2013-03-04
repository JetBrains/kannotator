package org.jetbrains.kannotator.classHierarchy

import kotlinlib.*
import java.util.*

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.graphs.*

data class ClassData(val name: ClassName, methodsById: Map<MethodId, Method>) {
    val methodsById = methodsById
}

fun buildClassHierarchyGraph(classSource: ClassSource): HierarchyGraph<ClassData> =
        ClassHierarchyBuilder(classSource).build()

class ClassHierarchyBuilder(val classSource: ClassSource): GraphBuilderImpl<ClassName, ClassData, Any?>(false, true) {
    override fun newGraph(): GraphImpl<ClassData, Any?> = HierarchyGraphImpl(createNodeMap)

    override fun newNode(name: ClassName) = ClassNodeImpl(name)

    override fun newEdge(label: Any?, from: NodeImpl<ClassData, Any?>, to: NodeImpl<ClassData, Any?>) =
            HierarchyEdgeImpl(from as ClassNodeImpl, to as ClassNodeImpl)

    fun getOrCreateEdge(parent: ClassNodeImpl, child: ClassNodeImpl): HierarchyEdgeImpl<ClassData> {
        return getOrCreateEdge(null, parent, child) as HierarchyEdgeImpl<ClassData>
    }

    fun build(): HierarchyGraph<ClassData> {
        classSource forEach {
            reader ->
            val className = ClassName.fromInternalName(reader.getClassName())
            val node = getOrCreateNode(className) as ClassNodeImpl
            val (methods, superClasses) = processClass(reader)
            for (superClass in superClasses) {
                val superClassNode = getOrCreateNode(superClass) as ClassNodeImpl
                getOrCreateEdge(parent = superClassNode, child = node)
            }
            for (method in methods) {
                node.addMethod(method)
            }
        }

        return toGraph() as HierarchyGraph<ClassData>
    }
}

class ClassNodeImpl(val name: ClassName): HierarchyNodeImpl<ClassData>() {
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

    public override fun toString(): String = name.internal
}

val Node<ClassData, *>.methods: Collection<Method>
    get() = data.methodsById.values()

val Node<ClassData, *>.name: ClassName
    get() = data.name

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
                    val method = Method(thisClassName, access, name, desc, signature)
                    methods.add(method)
                    return null
                }
            },
            flags(SKIP_CODE, SKIP_DEBUG, SKIP_FRAMES)
    )

    return MethodsAndSuperClasses(methods, superClasses)
}