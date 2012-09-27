package org.jetbrains.kannotator.funDependecy

import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.getArgumentTypes
import org.jetbrains.kannotator.declarations.getReturnType
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM4
import org.objectweb.asm.commons.Method as AsmMethod

public fun buildFunctionDependencyGraph(classReaders: List<ClassReader>) : FunDependencyGraph =
        FunDependencyGraphBuilder().build(classReaders)

fun Method.canBeAnnotated() : Boolean {
    return !id.getReturnType().isPrimitiveOrVoidType() || !id.getArgumentTypes().all { it.isPrimitiveOrVoidType() }
}

private class FunDependencyGraphBuilder() {
    private var currentFromNode : FunctionNodeImpl? = null
    private var currentClassName : ClassName? = null

    private val dependencyGraph = FunDependencyGraphImpl()
    private val methodSearcher = GlobalMethodSearcher()

    private val classVisitor = object : ClassVisitor(ASM4) {
        public override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
            currentClassName = ClassName.fromInternalName(name)
        }

        public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val method = Method(currentClassName!!, access, name, desc, signature)
            if (method.canBeAnnotated()) {
                currentFromNode = dependencyGraph.getOrCreateNode(method)
                return methodVisitor
            }

            return null
        }
    }

    private val methodVisitor = object : MethodVisitor(ASM4) {
        public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
            val method = methodSearcher.find(owner, name, desc)
            if (method != null && method.canBeAnnotated()) {
                dependencyGraph.createEdge(currentFromNode!!, dependencyGraph.getOrCreateNode(method))
            }
        }
    }

    public fun build(val classReaders: List<ClassReader>) : FunDependencyGraph {
        for (classReader in classReaders) {
            classReader.accept(classVisitor, SKIP_DEBUG or SKIP_FRAMES)
        }

        return dependencyGraph
    }
}