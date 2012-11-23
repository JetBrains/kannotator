package org.jetbrains.kannotator.controlFlow

import java.util.ArrayList
import java.util.LinkedHashSet
import java.util.HashMap
import org.jetbrains.kannotator.util.DataHolderImpl

public class ControlFlowGraphBuilder<L: Any> {
    private var result: ControlFlowGraph? = null

    private var entryPoint: Instruction? = null
    private val instructions: MutableCollection<InstructionImpl> = LinkedHashSet()
    private val labelInstructions: MutableMap<L, InstructionImpl> = HashMap()

    private fun checkFinished() {
        if (result != null) throw IllegalStateException("This builder has already finished")
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
            newInstruction(LabelMetadata(labelInstructions.size(), label)) as InstructionImpl
        }
    }

    public fun addInstruction(insn: Instruction) {
        instructions.add(insn as InstructionImpl)
    }

    public fun addEdge(from: Instruction, to: Instruction, exception: Boolean, state: State) {
        checkFinished()

        val edge = ControlFlowEdgeImpl(from, to, exception, state)

        (from as InstructionImpl).outgoingEdges.add(edge)
        (to as InstructionImpl).incomingEdges.add(edge)
    }

    public fun build(): ControlFlowGraph {
        if (result != null) return result!!

        result = object : ControlFlowGraph {
            override val instructions: Collection<Instruction> = this@ControlFlowGraphBuilder.instructions
            override val entryPoint: Instruction = this@ControlFlowGraphBuilder.entryPoint!!

            public fun toString(): String = "entryPoint=${entryPoint.metadata}, instructions=$instructions"
        }
        return result!!
    }
}

private class ControlFlowGraphImpl(
        override val entryPoint: Instruction,
        override val instructions: Collection<Instruction>
) : ControlFlowGraph {}

private class InstructionImpl(
        override val metadata: InstructionMetadata
) : DataHolderImpl<Instruction>(), Instruction {
    override val incomingEdges: MutableCollection<ControlFlowEdge> = ArrayList()
    override val outgoingEdges: MutableCollection<ControlFlowEdge> = ArrayList()

    public fun toString(): String = "[$metadata]{in:$incomingEdges, out:$outgoingEdges}"
}

private class ControlFlowEdgeImpl(
        override val from: Instruction,
        override val to: Instruction,
        override val exception: Boolean,
        override val state: State
) : ControlFlowEdge {
    public fun toString(): String = "${from.metadata} -> ${to.metadata}"
}

public class LabelMetadata<L: Any>(val id: Int, val label: L): InstructionMetadata {
    public fun toString(): String = "L$id"
}

