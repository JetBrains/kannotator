package tests.funDependency

import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.funDependecy.FunDependencyEdge
import org.jetbrains.kannotator.funDependecy.FunDependencyGraph
import org.jetbrains.kannotator.funDependecy.FunctionNode
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph

fun FunDependencyGraph.toJungGraph(): DirectedSparseMultigraph<FunctionNode, FunDependencyEdge> {
    val jungGraph = DirectedSparseMultigraph<FunctionNode, FunDependencyEdge>()
    for (i in this.functions) {
        for (e in i.outgoingEdges) {
            jungGraph.addEdge(e, e.from, e.to, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

fun main(args: Array<String>) {
    val listOfFiles = array("fundependency.recursiveFun.First", "fundependency.recursiveFun.Second")
    val graph = buildFunctionDependencyGraph(listOfFiles)
    displayJungGraph<FunctionNode, FunDependencyEdge>(
            graph.toJungGraph(),
            object : Transformer<FunctionNode, String> {
                public override fun transform(functionNode: FunctionNode): String = functionNode.method.toString()
            },
            null
    )
}
