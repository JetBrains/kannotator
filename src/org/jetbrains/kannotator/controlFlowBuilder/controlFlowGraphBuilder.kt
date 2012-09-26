package org.jetbrains.kannotator.controlFlowBuilder

import java.util.ArrayList
import java.util.HashSet
import kotlinlib.*
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.ControlFlowGraphBuilder
import org.jetbrains.kannotator.controlFlow.DataKey
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import org.jetbrains.kannotator.controlFlow.State
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.util.Printer

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
            val ownerClassName = ClassName.fromInternalName(owner)
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

public val STATE_BEFORE: DataKey<Instruction, State<Unit>> = DataKey()

class GraphBuilderMethodVisitor(
        val owner: ClassName,
        val graphBuilder: ControlFlowGraphBuilder<Label>,
        val methodNode: MethodNode,
        val callbacks: GraphBuilderCallbacks
) : MethodVisitor(ASM4, methodNode) {

    public override fun visitEnd() {
        try {
            super.visitEnd()
            val analyzer = GraphBuilderAnalyzer(graphBuilder, owner, methodNode)
            analyzer.analyze(owner.internal, methodNode)

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
            callbacks.error(owner.internal, methodNode.name, methodNode.desc, e)
        }
        callbacks.exitMethod(owner.internal, methodNode.name, methodNode.desc)
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

fun Method(className: ClassName, methodNode: MethodNode): Method = Method(
        className, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature)

private class GraphBuilderAnalyzer(
        val graph: ControlFlowGraphBuilder<Label>,
        val owner: ClassName,
        val methodNode: MethodNode
) : Analyzer<PossibleTypedValues>(GraphBuilderInterpreter(Method(owner, methodNode))) {

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