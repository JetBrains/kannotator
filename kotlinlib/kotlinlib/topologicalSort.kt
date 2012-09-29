package kotlinlib

import java.util.LinkedList
import java.util.HashSet

fun <T> Iterable<T>.topologicallySort(adjacentNodes: (T) -> Iterable<T>): List<T> {
    val result = LinkedList<T>()
    val visited = HashSet<T>()

    fun processNode(node: T) {
        if (!visited.add(node)) return
        for (next in adjacentNodes(node)) {
            processNode(next)
        }
        result.addFirst(node)
    }

    for (node in this) {
        processNode(node)
    }

    return result
}