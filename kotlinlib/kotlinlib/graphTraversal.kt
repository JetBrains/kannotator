package kotlinlib

import java.util.LinkedHashSet
import java.util.HashSet

trait TraversalContextApi<in T> {
    fun schedule(t: T)
    fun scheduleAll(t: Collection<T>)
}

trait TraversalContext<T>: TraversalContextApi<T> {
    fun next(): T
    fun isEmpty(): Boolean
}

class VisitOnceQueue<T>: TraversalContext<T> {
    private val data = LinkedHashSet<T>()

    override fun next(): T = data.removeFirst()
    override fun isEmpty(): Boolean = data.isEmpty()

    override fun schedule(t: T) {
        data.add(t)
    }

    override fun scheduleAll(t: Collection<T>) {
        data.addAll(t)
    }
}

fun <T> bfs(
        startFrom: Collection<T>,
        context: TraversalContext<T> = VisitOnceQueue<T>(),
        visited: MutableSet<T> = HashSet(),
        body: TraversalContextApi<T>.(current: T) -> Unit
): Set<T> {
    context.scheduleAll(startFrom)

    while (!context.isEmpty()) {
        val current = context.next()
        if (!visited.add(current)) continue

        context.body(current)
    }

    return visited
}
