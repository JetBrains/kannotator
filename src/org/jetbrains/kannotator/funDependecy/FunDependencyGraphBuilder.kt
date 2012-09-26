package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM4
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.tree.MethodNode
import java.io.InputStream
import org.jetbrains.kannotator.declarations.ClassName
import java.util.Collections
import org.jetbrains.kannotator.declarations.MethodId

fun buildFunctionDependencyGraph(classReaders: List<ClassReader>): FunDependencyGraph {
    val dependencyGraph = FunDependencyGraphImpl()

    for (classReader in classReaders) {
        val classVisitor = GraphBuilderClassVisitor(classReader.getClassName(), dependencyGraph)
        classReader.accept(classVisitor, 0)
    }
    return dependencyGraph
}

private class GraphBuilderClassVisitor(val className: String, val graph: FunDependencyGraphImpl): ClassVisitor(ASM4) {

    public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val method = Method(ClassName.fromInternalName(className), MethodId(name, desc))
        if (method.isNeedAnnotating()) {
            return GraphBuilderMethodVisitor(graph.getOrCreateNode(method), graph)
        }

        return null
    }
}

private class GraphBuilderMethodVisitor(
        val ownerMethod: FunctionNodeImpl,
        val graph: FunDependencyGraphImpl
): MethodVisitor(ASM4) {
    public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
        val method = Method(ClassName.fromInternalName(owner), MethodId(name, desc))
        if (method.isNeedAnnotating()) {
            graph.createEdge(ownerMethod, graph.getOrCreateNode(method))
        }
    }
}