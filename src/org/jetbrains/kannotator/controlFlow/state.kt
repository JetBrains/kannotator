package org.jetbrains.kannotator.controlFlow

public trait State<VI> {
    val stack: Stack
    val localVariables: LocalVariableTable
    fun valueInfo(value: Value): VI
}

public trait Stack {
    val size: Int
    fun get(indexFromTop: Int): Set<Value>
}

public trait LocalVariableTable {
    val size: Int
    fun get(variableIndex: Int): Set<Value>
}

public trait Value {

}