package org.jetbrains.kannotator.funDependecy

import java.util.ArrayList
import java.util.HashMap
import kotlinlib.flags
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Field
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.objectweb.asm.ClassReader.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM4
import org.objectweb.asm.commons.Method as AsmMethod
import org.jetbrains.kannotator.declarations.getType
import org.objectweb.asm.Type

public fun buildFunctionDependencyGraph(declarationIndex: DeclarationIndex, classSource: ClassSource) : DependencyGraph<Method> =
        FunDependencyGraphBuilder(declarationIndex, classSource, buildFieldsDependencyInfos(declarationIndex, classSource)).build()

private class FunDependencyGraphBuilder(
        private val declarationIndex: DeclarationIndex,
        private val classSource: ClassSource,
        private val fieldsDependencyInfos: Map<Field, FieldDependencyInfo>
) {
    private var currentFromNode : DependencyNodeImpl<Method>? = null
    private var currentClassName : ClassName? = null

    private val dependencyGraph = DependencyGraphImpl<Method>()

    private val classVisitor = object : ClassVisitor(ASM4) {
        public override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
            currentClassName = ClassName.fromInternalName(name)
        }

        public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val method = Method(currentClassName!!, access, name, desc, signature)
            currentFromNode = dependencyGraph.getOrCreateNode(method)
            return methodVisitor
        }
    }

    private val methodVisitor = object : MethodVisitor(ASM4) {
        public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
            val method = declarationIndex.findMethod(ClassName.fromInternalName(owner), name, desc)
            if (method != null) {
                dependencyGraph.createEdge(currentFromNode!!, dependencyGraph.getOrCreateNode(method), "call")
            }
        }
    }

    public fun build(): DependencyGraph<Method> {
        classSource.forEach {
            reader ->
            reader.accept(classVisitor, flags(SKIP_DEBUG, SKIP_FRAMES))
        }

        // Make all getter nodes depend on setters node
        for (fieldInfo in fieldsDependencyInfos.values()) {
            val fieldType = fieldInfo.field.getType().getSort()
            if (fieldType != Type.ARRAY && fieldType != Type.OBJECT) {
                continue
            }
            for (readerFun in fieldInfo.readers) {
                val readerNode = dependencyGraph.getOrCreateNode(readerFun)
                for (writerFun in fieldInfo.writers) {
                    if (writerFun != readerFun) {
                        val writerNode = dependencyGraph.getOrCreateNode(writerFun)
                        dependencyGraph.createEdge(readerNode, writerNode, "reading '${fieldInfo.field.name}'")
                    }
                }
            }
        }

        return dependencyGraph
    }
}