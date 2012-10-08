package util.controlFlow

import java.util.ArrayList
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.ControlFlowGraphBuilder
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.tree.MethodNode
import org.jetbrains.kannotator.controlFlow.builder.buildControlFlowGraph

public open class GraphBuilderCallbacks {
    open fun beforeMethod(internalClassName: String, methodName: String, methodDesc: String): Boolean = true
    open fun exitMethod(internalClassName: String, methodName: String, methodDesc: String) {}
    open fun error(internalClassName: String, methodName: String, methodDesc: String, e: Throwable) {
        throw e
    }
}

public fun buildControlFlowGraph(classReader: ClassReader, _methodName: String, _methodDesc: String): ControlFlowGraph {
    return buildGraphsForAllMethods(classReader, object : GraphBuilderCallbacks() {
        override fun beforeMethod(internalClassName: String, methodName: String, methodDesc: String): Boolean {
            return methodName == _methodName && methodDesc == _methodDesc
        }
    }).first!!.graph.build()
}

public data class MethodAndGraph(val method: Method, val graph: ControlFlowGraphBuilder<*>)

public fun buildGraphsForAllMethods(
        classReader: ClassReader,
        callbacks: GraphBuilderCallbacks = GraphBuilderCallbacks()
): List<MethodAndGraph> {
    val result = ArrayList<MethodAndGraph>()
    classReader.accept(object : ClassVisitor(ASM4) {

        public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val owner = classReader.getClassName()
            val proceed = callbacks.beforeMethod(owner, name, desc)
            if (!proceed) return null

            val ownerClassName = ClassName.fromInternalName(owner)
            val builder = ControlFlowGraphBuilder<Label>()
            result.add(MethodAndGraph(Method(ownerClassName, access, name, desc), builder))

            val methodNode = MethodNode(access, name, desc, signature, exceptions)
            return GraphBuilderMethodVisitor(
                    ownerClassName,
                    builder,
                    methodNode,
                    callbacks
            )
        }
    }, 0)
    return result
}

class GraphBuilderMethodVisitor(
        val owner: ClassName,
        val graphBuilder: ControlFlowGraphBuilder<Label>,
        val methodNode: MethodNode,
        val callbacks: GraphBuilderCallbacks
) : MethodVisitor(ASM4, methodNode) {

    public override fun visitEnd() {
        try {
            super.visitEnd()
            methodNode.buildControlFlowGraph(owner, graphBuilder)
        }
        catch (e: Throwable) {
            callbacks.error(owner.internal, methodNode.name, methodNode.desc, e)
        }
        callbacks.exitMethod(owner.internal, methodNode.name, methodNode.desc)
    }
}