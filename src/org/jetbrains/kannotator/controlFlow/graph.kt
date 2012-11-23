package org.jetbrains.kannotator.controlFlow

import org.jetbrains.kannotator.util.DataHolder

public trait ControlFlowGraph {
    val instructions: Collection<Instruction>
    val entryPoint: Instruction
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