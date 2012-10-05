package org.jetbrains.kannotator.controlFlowBuilder

import java.util.HashSet
import kotlinlib.*
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.ControlFlowGraphBuilder
import org.jetbrains.kannotator.controlFlow.DataKey
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import org.jetbrains.kannotator.controlFlow.State
import org.jetbrains.kannotator.declarations.ClassName
import org.jetbrains.kannotator.declarations.Method
import org.objectweb.asm.Label
import org.objectweb.asm.commons.Method as AsmMethod
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.util.Printer

public fun MethodNode.buildControlFlowGraph(
        owner: ClassName,
        graphBuilder: ControlFlowGraphBuilder<Label> = ControlFlowGraphBuilder()
): ControlFlowGraph {
    val analyzer = GraphBuilderAnalyzer(graphBuilder, owner, this)
    analyzer.analyze(owner.internal, this)

    for ((index, inst) in analyzer.instructions.indexed) {
        val frame = analyzer.getFrames()[index]
        if (frame != null) {
            inst[STATE_BEFORE] = FrameState(frame)
        }
    }
    return graphBuilder.build()
}

public val STATE_BEFORE: DataKey<Instruction, State> = DataKey()

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

private fun Method(className: ClassName, methodNode: MethodNode): Method = Method(
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