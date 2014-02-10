package tests.funDependency

import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import java.io.File
import java.util.ArrayList
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.util.processJar
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import util.ClassPathDeclarationIndex
import org.jetbrains.kannotator.declarations.Method
import org.jetbrains.kannotator.graphs.Node
import org.jetbrains.kannotator.graphs.Edge
import org.jetbrains.kannotator.graphs.Graph

fun <A, L> Graph<A, L>.toJungGraph(): DirectedSparseMultigraph<Node<A, L>, Edge<A, L>> {
    val jungGraph = DirectedSparseMultigraph<Node<A, L>, Edge<A, L>>()
    for (i in this.nodes) {
        for (e in i.outgoingEdges) {
            jungGraph.addEdge(e, e.from, e.to, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

/** displays dependencies (via jung) for kotlin-runtime */
fun main(args: Array<String>) {
    val file = File("lib/kotlin-runtime.jar")

    val classSource = FileBasedClassSource(arrayList(file))
    val graph = buildFunctionDependencyGraph(ClassPathDeclarationIndex, classSource)
    displayJungGraph(
            graph.toJungGraph(),
            object : Transformer<Node<Method, String>, String> {
                public override fun transform(functionNode: Node<Method, String>): String = functionNode.data.toString()
            },
            null
    )
}
