package org.jetbrains.kannotator.controlFlow

import java.util.ArrayList
import java.util.LinkedHashSet
import java.util.HashMap
import org.jetbrains.kannotator.util.DataHolderImpl
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.kannotator.annotationsInference.forInterestingValue
import org.jetbrains.kannotator.annotationsInference.traverseInstructions
import org.jetbrains.kannotator.asm.util.getAsmInstructionNode
import org.jetbrains.kannotator.asm.util.getOpcode
import org.jetbrains.kannotator.asm.util.isPrimitiveOrVoidType
import org.jetbrains.kannotator.controlFlow.ControlFlowGraph
import org.jetbrains.kannotator.controlFlow.Instruction
import org.jetbrains.kannotator.controlFlow.ControlFlowEdge
import org.jetbrains.kannotator.controlFlow.builder.STATE_BEFORE
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.index.DeclarationIndex
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.FieldInsnNode
import java.util.LinkedHashSet
import org.jetbrains.kannotator.controlFlow.InstructionMetadata
import org.jetbrains.kannotator.util.DataKey
import java.util.ArrayList
import java.util.Collections
import kotlinlib.removeLast
import java.util.ArrayDeque

private class InstructionOutcomeDataImpl: InstructionOutcomeData {
    val map = HashMap<Instruction, MethodOutcome>()

    override fun get(insn: Instruction): MethodOutcome {
        return map.getOrPut(insn, {insn.computeOutcome()})
    }
}

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
            override val instructionOutcomes: InstructionOutcomeData = InstructionOutcomeDataImpl()

            public fun toString(): String = "entryPoint=${entryPoint.metadata}, instructions=$instructions"
        }
        return result!!
    }
}

private val RETURN_OPCODES = hashSet(ARETURN, RETURN, IRETURN, LRETURN, DRETURN, FRETURN)

private fun Instruction.computeOutcome(): MethodOutcome {
    fun MethodOutcome?.merge(other: MethodOutcome): MethodOutcome {
        return if (this == null)
            other
        else if (this == other)
            this
        else
            MethodOutcome.RETURNS_AND_THROWS
    }

    var result: MethodOutcome? = null

    val visited = HashSet<Instruction>()
    val stack = ArrayDeque<Instruction>()

    stack.push(this);
    while (!(result == MethodOutcome.RETURNS_AND_THROWS || stack.isEmpty())) {
        val insn = stack.pop()
        if (RETURN_OPCODES.contains(insn.getOpcode())) {
            result = result merge MethodOutcome.ONLY_RETURNS
        } else if (insn.getOpcode() == ATHROW) {
            result = result merge MethodOutcome.ONLY_THROWS
        }
        visited.add(insn)
        for (e in insn.outgoingEdges) {
            val nextInsn = e.to
            if (!visited.contains(nextInsn)) {
                stack.push(nextInsn);
            }
        }
    }

    return if (result != null) result!! else MethodOutcome.ONLY_RETURNS
}

private class ControlFlowGraphImpl(
        override val entryPoint: Instruction,
        override val instructions: Collection<Instruction>,
        override val instructionOutcomes: InstructionOutcomeData
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

