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

public fun buildControlFlowGraph(classReader: ClassReader, methodName: String, methodDesc: String): ControlFlowGraph {
    val classVisitor = GraphBuilderClassVisitor(classReader.getClassName()!!, methodName, methodDesc)
    classReader.accept(classVisitor, 0)
    return classVisitor.graph
}

private class GraphBuilderClassVisitor(val className: String, val methodName: String, val methodDesc: String) : ClassVisitor(ASM4) {

    private val graphBuilder = ControlFlowGraphBuilder<Label>()

    val graph: ControlFlowGraph
        get() = graphBuilder.build()

    public override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val mv = super.visitMethod(access, name, desc, signature, exceptions)
        println(name + desc)
        if (name != methodName || desc != methodDesc) {
            return mv
        }
        val methodNode = MethodNode(access, name, desc, signature, exceptions)
        return GraphBuilderMethodVisitor(className, graphBuilder, methodNode)
    }
}

private class GraphBuilderMethodVisitor(
        val owner: String,
        val graphBuilder: ControlFlowGraphBuilder<Label>,
        val methodNode: MethodNode
) : MethodVisitor(ASM4, methodNode) {

    public override fun visitEnd() {
        super.visitEnd()
        val analyzer = GraphBuilderAnalyzer(graphBuilder, methodNode)
        analyzer.analyze(owner, methodNode)
        val frames = analyzer.getFrames()
        val instructions = methodNode.instructions
        for ((i, frame) in frames.indexed) {
            if (frame == null) {
                println("Unreachable: ${instructions[i]}")
            }
            else {
                println(instructions[i])
            }
        }
    }
}


private class GraphBuilderAnalyzer(val graph: ControlFlowGraphBuilder<Label>, val methodNode: MethodNode) : Analyzer<BasicValue>(GraphBuilderInterpreter()) {
    private val instructions = methodNode.instructions.iterator().map { it -> it.toInstruction() }.toArrayList()

    fun AbstractInsnNode.toInstruction(): Instruction {
        if (this is LabelNode) {
            return graph.getLabelInstruction(this.getLabel())
        }
        return graph.newInstruction(Metadata(this))
    }


    {
        graph.setEntryPoint(instructions[0])
    }


    private class Metadata(val asmInstruction: AbstractInsnNode) : InstructionMetadata {
        public fun toString(): String {
            val opcode = asmInstruction.getOpcode()
            if (opcode == -1) {
                return when (asmInstruction) {
                    is LineNumberNode -> "Line num: ${asmInstruction.line}"
                    is FrameNode -> "frame node"
                    else -> asmInstruction.toString()
                }
            }
            return Printer.OPCODES[opcode]!!
        }

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
        graph.addEdge(instructions[from], instructions[to])
    }

}

private class GraphBuilderInterpreter: BasicInterpreter() {

}
