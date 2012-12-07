package tests.funDependency

import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import java.io.File
import java.util.ArrayList
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.funDependecy.DependencyEdge
import org.jetbrains.kannotator.funDependecy.DependencyGraph
import org.jetbrains.kannotator.funDependecy.DependencyNode
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.util.processJar
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import util.ClassPathDeclarationIndex
import org.jetbrains.kannotator.declarations.Method

fun <A> DependencyGraph<A>.toJungGraph(): DirectedSparseMultigraph<DependencyNode<A>, DependencyEdge<A>> {
    val jungGraph = DirectedSparseMultigraph<DependencyNode<A>, DependencyEdge<A>>()
    for (i in this.allNodes) {
        for (e in i.outgoingEdges) {
            jungGraph.addEdge(e, e.from, e.to, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

fun main(args: Array<String>) {
    val file = File("lib/kotlin-runtime.jar")

    val classSource = FileBasedClassSource(arrayList(file))
    val graph = buildFunctionDependencyGraph(ClassPathDeclarationIndex, classSource)
    displayJungGraph<DependencyNode<Method>, DependencyEdge<Method>>(
            graph.toJungGraph(),
            object : Transformer<DependencyNode<Method>, String> {
                public override fun transform(functionNode: DependencyNode<Method>): String = functionNode.data.toString()
            },
            null
    )
}
