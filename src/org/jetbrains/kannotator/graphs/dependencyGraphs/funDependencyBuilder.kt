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
import org.jetbrains.kannotator.graphs.NodeImpl
import org.jetbrains.kannotator.graphs.GraphBuilder
import org.jetbrains.kannotator.graphs.DefaultNodeImpl
import org.jetbrains.kannotator.graphs.GraphImpl
import org.jetbrains.kannotator.graphs.Graph
import org.jetbrains.kannotator.declarations.Access
import org.jetbrains.kannotator.declarations.ClassMember

public fun buildFunctionDependencyGraph(declarationIndex: DeclarationIndex, classSource: ClassSource) : Graph<Method, String> =
        FunDependencyGraphBuilder(declarationIndex, classSource, buildFieldsDependencyInfos(declarationIndex, classSource)).build()

public class FunDependencyGraphBuilder(
        private val declarationIndex: DeclarationIndex,
        private val classSource: ClassSource,
        private val fieldsDependencyInfos: Map<Field, FieldDependencyInfo>,
        private val onMissingDependency: (ClassMember) -> ClassMember? = {null}
): GraphBuilder<Method, Method, String, GraphImpl<Method, String>>(false, true) {
    private var currentFromNode : NodeImpl<Method, String>? = null
    private var currentClassName : ClassName? = null

    private val classVisitor = object : ClassVisitor(ASM4) {
        public override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
            currentClassName = ClassName.fromInternalName(name)
        }

        public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val method = Method(currentClassName!!, access, name, desc, signature)
            currentFromNode = getOrCreateNode(method)
            return methodVisitor
        }
    }

    private val methodVisitor = object : MethodVisitor(ASM4) {
        public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String) {
            val ownerClassName = ClassName.fromInternalName(owner)
            val method = declarationIndex.findMethod(ownerClassName, name, desc)
                         ?: onMissingDependency(Method(ownerClassName, 0, name, desc)) as Method?
            if (method != null) {
                getOrCreateEdge("call", currentFromNode!!, getOrCreateNode(method))
            }
        }


        public override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
            // This is only needed for dependency detection
            val ownerClassName = ClassName.fromInternalName(owner)
            if (declarationIndex.findField(ownerClassName, name) == null) {
                onMissingDependency(Field(ownerClassName, 0, name, desc))
            }
        }
    }


    override fun newGraph(): GraphImpl<Method, String> = GraphImpl(false)
    override fun newNode(data: Method): NodeImpl<Method, String> = DefaultNodeImpl(data)

    public fun build(): Graph<Method, String> {
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
                val readerNode = getOrCreateNode(readerFun)
                for (writerFun in fieldInfo.writers) {
                    if (writerFun != readerFun) {
                        val writerNode = getOrCreateNode(writerFun)
                        getOrCreateEdge("reading '${fieldInfo.field.name}'", readerNode, writerNode)
                    }
                }
            }
        }

        return toGraph()
    }
}