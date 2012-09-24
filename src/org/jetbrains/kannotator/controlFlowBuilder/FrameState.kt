package org.jetbrains.kannotator.controlFlowBuilder

import org.jetbrains.kannotator.controlFlow.State
import org.jetbrains.kannotator.controlFlow.Value
import org.jetbrains.kannotator.controlFlow.LocalVariableTable
import org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.kannotator.controlFlow.Stack

public abstract class FrameState<VI>(private val frame: Frame<PossibleTypedValues>): State<VI> {
    override val localVariables: LocalVariableTable = object : LocalVariableTable {
        override fun get(variableIndex: Int): Set<Value>  = frame.getLocal(variableIndex)?.values ?: hashSet()
        override val size = frame.getLocals()
    }

    override val stack: Stack = object : Stack {
        override fun get(indexFromTop: Int): Set<Value> = frame.getStack(indexFromTop)?.values ?: hashSet()
        override val size = frame.getStackSize()
    }
}