package tests.funDependency

import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import java.io.File
import java.util.ArrayList
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.funDependecy.FunDependencyEdge
import org.jetbrains.kannotator.funDependecy.FunDependencyGraph
import org.jetbrains.kannotator.funDependecy.FunctionNode
import org.jetbrains.kannotator.funDependecy.buildFunctionDependencyGraph
import org.objectweb.asm.ClassReader
import org.jetbrains.kannotator.util.processJar
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.index.DeclarationIndexImpl
import util.ClassPathDeclarationIndex
import org.jetbrains.kannotator.declarations.Method

fun <A> FunDependencyGraph<A>.toJungGraph(): DirectedSparseMultigraph<FunctionNode<A>, FunDependencyEdge<A>> {
    val jungGraph = DirectedSparseMultigraph<FunctionNode<A>, FunDependencyEdge<A>>()
    for (i in this.functions) {
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
    displayJungGraph<FunctionNode<Method>, FunDependencyEdge<Method>>(
            graph.toJungGraph(),
            object : Transformer<FunctionNode<Method>, String> {
                public override fun transform(functionNode: FunctionNode<Method>): String = functionNode.data.toString()
            },
            null
    )
}
