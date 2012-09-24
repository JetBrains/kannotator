package org.jetbrains.kannotator.controlFlow

public trait ControlFlowGraph {
    val instructions: Collection<Instruction>
    val entryPoint: Instruction
}

public trait ControlFlowEdge {
    val from: Instruction
    val to: Instruction
}

public trait Instruction : DataHolder<Instruction> {
    val incomingEdges: Collection<ControlFlowEdge>
    val outgoingEdges: Collection<ControlFlowEdge>
    val metadata: InstructionMetadata
}

public trait InstructionMetadata {

}