package org.jetbrains.kannotator.controlFlow

import kotlinlib.IndexedElement
import kotlinlib.indexedIterator

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



public val LocalVariableTable.indexed: Iterator<IndexedElement<Set<Value>>>
        get() = indexedIterator(this, size) { c, i -> c.get(i) }

public val Stack.indexed: Iterator<IndexedElement<Set<Value>>>
        get() = indexedIterator(this, size) { c, i -> c.get(i) }
