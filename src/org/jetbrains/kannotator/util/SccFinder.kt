package funDependency

import java.util.HashMap
import java.util.HashSet
import java.util.IdentityHashMap
import java.util.LinkedHashSet
import java.util.Stack
import java.util.ArrayList
import com.gs.collections.impl.set.strategy.mutable.MutableHashingStrategySetFactoryImpl
import com.gs.collections.api.block.HashingStrategy

/**
 * Tarjan's strongly connected components algorithm
 * http://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm
 */
public class SCCFinder<Graph, Node>(
        val graph: Graph,
        val graphNodes: (Graph) -> Iterable<Node>,
        val adjacentNodes: (Node) -> Iterable<Node>) {

    private class object {
        private fun <T: Any> identityHashSet(): MutableSet<T> = MutableHashingStrategySetFactoryImpl().with(object : HashingStrategy<T> {
            public override fun equals(object1: T?, object2: T?): Boolean = object1 identityEquals object2
            public override fun computeHashCode(`object`: T?): Int = System.identityHashCode(`object`)
        })!!
    }

    private class IdentityHashStack<T> {
        val stack = Stack<T>()
        val identitySet = identityHashSet<T>()

        fun contains(elem : T) : Boolean = identitySet.contains(elem)
        fun push(elem : T) {
            stack.push(elem)
            assert(identitySet.add(elem))
        }
        fun pop() : T {
            val elem = stack.pop()!!
            assert(identitySet.remove(elem))
            return elem
        }
        fun isEmpty() : Boolean {
            assert(stack.isEmpty() == identitySet.isEmpty())
            return stack.isEmpty()
        }
    }

    private val components = ArrayList<Set<Node>>()
    private val nodeToComponent = IdentityHashMap<Node, Set<Node>>()

    // Index of visited nodes
    private val nodeIndex = IdentityHashMap<Node, Int>()

    // Current index for not-visited node
    private var index = 0

    public fun findComponent(node: Node): Set<Node> {
        if (!nodeIndex.containsKey(node)) {
            execute(node)
        }

        return nodeToComponent[node] ?: throw IllegalStateException("Can't find component for node ${node}")
    }

    public fun getAllComponents(): List<Set<Node>> {
        for (node in graphNodes(graph)) {
            findComponent(node)
        }
        return components
    }

    private fun execute(node: Node) {
        val stack = IdentityHashStack<Node>();
        val minUnvisitedReachable = IdentityHashMap<Node, Int>()

        executeOnNode(node, stack, minUnvisitedReachable)

        assert(stack.isEmpty())
    }

    private fun executeOnNode(node: Node, stack: IdentityHashStack<Node>, minUnvisitedReachable: MutableMap<Node, Int>) {
        val currentNodeIndex = index

        minUnvisitedReachable[node] = currentNodeIndex
        nodeIndex[node] = currentNodeIndex

        index++

        stack.push(node);

        for (nextNode in adjacentNodes(node)) {
            val currentComponentIndex = minUnvisitedReachable[node]!!

            if (!nodeIndex.containsKey(nextNode)) {
                executeOnNode(nextNode, stack, minUnvisitedReachable);
                minUnvisitedReachable[node] = Math.min(currentComponentIndex, minUnvisitedReachable[nextNode]!!)
            } else if (stack.contains(nextNode)) {
                minUnvisitedReachable[node] = Math.min(currentComponentIndex, nodeIndex[nextNode]!!)
            }
        }

        if (minUnvisitedReachable[node] == currentNodeIndex){
            val component = identityHashSet<Node>()
            do {
                val componentNode = stack.pop()
                component.add(componentNode)
                nodeToComponent[componentNode] = component

                if (componentNode identityEquals node) {
                    break
                }
            } while(true)

            components.add(component)
        }
    }
}