package funDependency

import java.util.HashMap
import java.util.HashSet
import org.junit.Test
import org.junit.Assert
import com.sun.org.apache.xml.internal.utils.StringComparable
import java.util.Comparator

data class Node(val name: String)

class Graph {
    private val nodes = HashMap<String, Node>()
    private val outgoingEdges = HashMap<Node, MutableSet<Node>>()

    public fun getAllNodes(): List<Node> = nodes.values().toList()
    public fun getAdjacentNodes(n: Node): Set<Node> = outgoingEdges[n]!!
    public fun getNode(label: String): Node = nodes[label]!!
    public fun getNodes(vararg labels: String): List<Node> = labels.map { getNode(it) }

    fun nodes(vararg labels: String): Graph {
        for (label in labels) {
            node(label)
        }

        return this
    }

    fun node(label: String): Node {
        val n = Node(label)
        outgoingEdges[n] = HashSet()
        nodes[label] = n
        return n
    }

    fun edges(vararg fromToStr: String): Graph {
        for (fromToEdgeStr in fromToStr) {
            val split = fromToEdgeStr.split("\\-\\>")
            val fromLabel = split[0].trim()
            val toLabel = split[1].trim()

            edge(nodes[fromLabel]!!, nodes[toLabel]!!)
        }

        return this
    }

    fun edge(from: Node, to: Node) {
        outgoingEdges[from]!!.add(to)
    }
}

class SCCFinderTest {
    private val nodesComparator = object : Comparator<Node> {
        public override fun compare(o1: Node?, o2: Node?): Int {
            return o1?.name?.compareTo(o2?.name ?: "") ?: -1
        }
        public override fun equals(obj: Any?): Boolean {
            throw UnsupportedOperationException()
        }
    }

    Test fun test1() {
        val graph = Graph().nodes("A", "B", "C").edges("A->B", "B->C", "C->B")
        doTest(graph, "A->A", "B->B,C", "C->B,C")
    }

    Test fun test2() {
        val graph = Graph().nodes("A", "B", "C").edges("A->B", "C->C")
        doTest(graph, "A->A", "B->B", "C->C")
    }

    Test fun test3() {
        val graph = Graph().nodes("A", "B", "C").edges("A->B", "B->C", "C->A")
        doTest(graph, "A->A,B,C", "B->A,B,C", "C->A,B,C")
    }

    Test fun test4() {
        val graph = Graph().nodes("A", "B", "C", "D").edges("A->B", "B->C", "B->D", "C->A", "C->D")
        doTest(graph, "A->A,B,C", "B->A,B,C", "C->A,B,C", "D->D")
    }

    fun doTest(graph : Graph, vararg componentAssertion : String) {
        val finder = SCCFinder<Graph, Node>(graph, { graph.getAllNodes() }, { graph.getAdjacentNodes(it).toList() })

        fun assertInComponent(component : String) {
            val split = component.split("\\-\\>")
            val requestNode = graph.getNode(split[0].trim())
            val nodes = graph.getNodes(*(split[1].split("\\,") as Array<String>).map { it.trim() }.toArray(array<String>()))

            val componentNodes = finder.findComponent(requestNode)

            Assert.assertArrayEquals("Invalid component nodes set for ${requestNode} node",
                    nodes.sort(nodesComparator).toArray(),
                    componentNodes.sort(nodesComparator).toArray())
        }

        for (component in componentAssertion) {
            assertInComponent(component)
        }
    }
}