package org.jetbrains.kannotator.controlFlow

import java.util.ArrayList
import java.util.LinkedHashSet
import java.util.HashMap

public class ControlFlowGraphBuilder<L: Any> {

    private var finished: Boolean = false

    private var entryPoint: Instruction? = null
    private val instructions: MutableCollection<InstructionImpl> = LinkedHashSet()
    private val labelInstructions: MutableMap<L, InstructionImpl> = HashMap()

    private fun checkFinished() {
        if (finished) throw IllegalStateException("This builder has already finished")
    }

    public fun setEntryPoint(entryPoint: Instruction) {
        checkFinished()
        this.entryPoint = entryPoint;
    }

    public fun newInstruction(metadata: InstructionMetadata): Instruction {
        checkFinished()
        val inst = InstructionImpl(metadata)
        instructions.add(inst)
        return inst
    }

    public fun getLabelInstruction(label: L): Instruction {
        checkFinished()
        return labelInstructions.getOrPut(label) {
            newInstruction(LabelMetadata(label)) as InstructionImpl
        }
    }

    public fun addEdge(from: Instruction, to: Instruction) {
        checkFinished()
        instructions.add(from as InstructionImpl)
        instructions.add(to as InstructionImpl)

        val edge = ControlFlowEdgeImpl(from, to)

        from.outgoingEdges.add(edge)
        to.incomingEdges.add(edge)
    }

    public fun build(): ControlFlowGraph {
        checkFinished()
        finished = true
        return object : ControlFlowGraph {
            override val instructions: Collection<Instruction> = this@ControlFlowGraphBuilder.instructions
            override val entryPoint: Instruction = this@ControlFlowGraphBuilder.entryPoint!!

            public fun toString(): String = "entryPoint=${entryPoint.metadata}, instructions=$instructions"
        }
    }
}

private class ControlFlowGraphImpl(
        override val entryPoint: Instruction,
        override val instructions: Collection<Instruction>
) : ControlFlowGraph {}

private class InstructionImpl(
        override val metadata: InstructionMetadata
) : Instruction {
    override val incomingEdges: MutableCollection<ControlFlowEdge> = ArrayList()
    override val outgoingEdges: MutableCollection<ControlFlowEdge> = ArrayList()

    public fun toString(): String = "[$metadata]{in:$incomingEdges, out:$outgoingEdges}"
}

private class ControlFlowEdgeImpl(
        override val from: Instruction,
        override val to: Instruction
) : ControlFlowEdge {
    public fun toString(): String = "${from.metadata} -> ${to.metadata}"
}

private class LabelMetadata<L: Any>(val label: L): InstructionMetadata {
    public fun toString(): String = label.toString()
}

