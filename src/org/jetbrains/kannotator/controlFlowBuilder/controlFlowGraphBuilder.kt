package org.jetbrains.kannotator.controlFlowBuilder

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.util.Printer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.ClassNode

import kotlinlib.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.TryCatchBlockNode
import org.jetbrains.kannotator.controlFlow.ControlFlowGraphBuilder
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import java.util.HashMap
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import java.util.ArrayList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.Handle
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.kannotator.asm.util.toOpcodeString
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.InsnList
import java.util.HashSet
import org.jetbrains.kannotator.controlFlow.DataKey
import org.jetbrains.kannotator.controlFlow.State
import org.jetbrains.kannotator.controlFlow.Value
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Method as AsmMethod
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.MethodId

public fun buildControlFlowGraph(classReader: ClassReader, _methodName: String, _methodDesc: String): ControlFlowGraph {
    return buildGraphsForAllMethods(classReader, object : GraphBuilderCallbacks() {
        override fun beforeMethod(internalClassName: String, methodName: String, methodDesc: String): Boolean {
            return methodName == _methodName && methodDesc == _methodDesc
        }
    }).first!!.graph.build()
}

public data class MethodAndGraph(val method: Method, val graph: ControlFlowGraphBuilder<*>)

public open class GraphBuilderCallbacks {
    open fun beforeMethod(internalClassName: String, methodName: String, methodDesc: String): Boolean = true
    open fun exitMethod(internalClassName: String, methodName: String, methodDesc: String) {}
    open fun error(internalClassName: String, methodName: String, methodDesc: String, e: Throwable) {
        throw e
    }
}

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

            val builder = ControlFlowGraphBuilder<Label>()
            result.add(MethodAndGraph(Method(ClassName.fromInternalName(owner), MethodId(name, desc)), builder))

            val methodNode = MethodNode(access, name, desc, signature, exceptions)
            return GraphBuilderMethodVisitor(
                    owner,
                    builder,
                    methodNode,
                    callbacks
            )
        }
    }, 0)
    return result
}

fun methodKind(access: Int): MethodKind {
    return if (Opcodes.ACC_STATIC and access == 0) MethodKind.INSTANCE else MethodKind.STATIC
}

public val STATE_BEFORE: DataKey<Instruction, State<Unit>> = DataKey()

class GraphBuilderMethodVisitor(
        val ownerInternalName: String,
        val graphBuilder: ControlFlowGraphBuilder<Label>,
        val methodNode: MethodNode,
        val callbacks: GraphBuilderCallbacks
) : MethodVisitor(ASM4, methodNode) {

    public override fun visitEnd() {
        try {
            super.visitEnd()
            val analyzer = GraphBuilderAnalyzer(graphBuilder, methodNode)
            analyzer.analyze(ownerInternalName, methodNode)

            for ((index, inst) in analyzer.instructions.indexed) {
                val frame = analyzer.getFrames()[index]
                if (frame != null) {
                    inst[STATE_BEFORE] = object : FrameState<Unit>(frame!!) {
                        override fun valueInfo(value: Value) {}
                    }
                }
            }
        }
        catch (e: Throwable) {
            callbacks.error(ownerInternalName, methodNode.name, methodNode.desc, e)
        }
        callbacks.exitMethod(ownerInternalName, methodNode.name, methodNode.desc)
    }
}

class AsmInstructionMetadata(val asmInstruction: AbstractInsnNode) : InstructionMetadata {
    public fun toString(): String {
        val opcode = asmInstruction.getOpcode()
        if (opcode == -1) {
            return when (asmInstruction) {
                is LineNumberNode -> "Line num: ${asmInstruction.line}"
                is FrameNode -> "frame node"
                else -> asmInstruction.toString()!!
            }
        }
        return Printer.OPCODES[opcode]!!
    }
}

private class GraphBuilderAnalyzer(
        val graph: ControlFlowGraphBuilder<Label>,
        val methodNode: MethodNode
) : Analyzer<PossibleTypedValues>(GraphBuilderInterpreter(methodKind(methodNode.access), methodNode.desc)) {

    public val instructions: List<Instruction> = methodNode.instructions.iterator().map { it -> it.toInstruction() }.toArrayList();

    {
        if (instructions.size > 0) {
            graph.setEntryPoint(instructions[0])
        }
        else {
            graph.setEntryPoint(graph.newInstruction(object : InstructionMetadata {
                fun toString() = "Entry point of an empty method"
            }))
        }
    }

    private val edges: MutableSet<Pair<Instruction, Instruction>> = HashSet()

    fun AbstractInsnNode.toInstruction(): Instruction {
        if (this is LabelNode) {
            return graph.getLabelInstruction(this.getLabel())
        }
        return graph.newInstruction(AsmInstructionMetadata(this))
    }

    protected override fun newControlFlowExceptionEdge(insn: Int, successor: Int): Boolean {
        createEdge(insn, successor)
        return super<Analyzer>.newControlFlowExceptionEdge(insn, successor)
    }

    protected override fun newControlFlowEdge(insn: Int, successor: Int) {
        createEdge(insn, successor)
        super<Analyzer>.newControlFlowEdge(insn, successor)
    }

    private fun createEdge(from: Int, to: Int) {
        if (edges.add(Pair(instructions[from], instructions[to]))) {
            graph.addEdge(instructions[from], instructions[to])
        }
    }

}