package org.jetbrains.kannotator.controlFlow

import org.jetbrains.kannotator.util.DataHolder

public trait ControlFlowGraph {
    val instructions: Collection<Instruction>
    val entryPoint: Instruction
    val instructionOutcomes: InstructionOutcomeData
}

public enum class MethodOutcome {
    ONLY_RETURNS
    ONLY_THROWS
    RETURNS_AND_THROWS
}

public trait InstructionOutcomeData {
    fun get(insn: Instruction): MethodOutcome
}

public trait ControlFlowEdge {
    val from: Instruction
    val to: Instruction
    val exception: Boolean
    val state: State
}

public trait Instruction : DataHolder<Instruction> {
    val incomingEdges: Collection<ControlFlowEdge>
    val outgoingEdges: Collection<ControlFlowEdge>
    val metadata: InstructionMetadata
}

public trait InstructionMetadata {

}