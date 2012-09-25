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
import org.jetbrains.kannotator.declarations.ClassName

fun buildFunctionDependencyGraph(classes: Array<String>): FunDependencyGraph {
    val functions = hashMap<Method, FunctionNodeImpl>()
    for (klass in classes) {
        val classReader = ClassReader(klass)
        val classVisitor = GraphBuilderClassVisitor(classReader.getClassName()!!, functions)
        classReader.accept(classVisitor, 0)
    }

    val list = ArrayList<FunctionNode>()
    list.addAll(functions.values())
    return object : FunDependencyGraph {
        override val functions: List<FunctionNode> = list
    }
}

private class GraphBuilderClassVisitor(val className: String, val functions: MutableMap<Method, FunctionNodeImpl>): ClassVisitor(ASM4) {

    public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        super.visitMethod(access, name, desc, signature, exceptions)
        val methodNode = MethodNode(access, name, desc, signature, exceptions)

        val method = Method.create(ClassName.fromCanonicalName(className), name, desc)
        val functionNode = functions.getOrPut(method, { FunctionNodeImpl(method) })
        return GraphBuilderMethodVisitor(functionNode, functions, methodNode)
    }
}

private class GraphBuilderMethodVisitor(
        val ownerMethod: FunctionNodeImpl,
        val functions: MutableMap<Method, FunctionNodeImpl>,
        val methodNode: MethodNode
): MethodVisitor(ASM4, methodNode) {
    public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
        super<MethodVisitor>.visitMethodInsn(opcode, owner, name, desc)
        val method = Method.create(ClassName.fromCanonicalName(owner), name, desc)
        val functionNode = functions.getOrPut(method, { FunctionNodeImpl(method) })
        addEdge(ownerMethod, functionNode)
    }
}