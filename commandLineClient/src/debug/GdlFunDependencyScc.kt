package debug.gdlFunDependencyScc

import java.io.File
import org.jetbrains.kannotator.index.*
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.*
import org.jetbrains.kannotator.funDependecy.*
import org.jetbrains.kannotator.graphs.Graph

/** dumps class hierarchy for a jar in gdl format
 * main jar gdlFile
 * */
fun main(args: Array<String>) {
    val jar = File(args[0])
    val gdl = File(args[1])

    val classSource: ClassSource = FileBasedClassSource(listOf(jar))
    val declarationIndex = DeclarationIndexImpl(classSource)

    val depGraph: Graph<Method, String> = buildFunctionDependencyGraph(declarationIndex, classSource)
    val components = depGraph.getTopologicallySortedStronglyConnectedComponents().reverse()

    val sb = StringBuilder()

    sb.append("""
    graph: {
    layoutalgorithm: minbackward
    orientation: left_to_right
    cmin: 1000
    crossing_weight: medianbary
    """)

    components.forEachWithIndex { i, nodes ->
        sb.append(
        """
        graph: {
        title: "scc_${i}"
        state: ${if (nodes.size() > 1) "boxed" else "folded"}
        """)
        if (nodes.size() > 1) {
            sb.append("""
        color: red
        """)
        }
        nodes.forEach {
            node ->
            sb.append(
            """
            node: {title:"${node.data}" label:"${node.data}"}
            """)

            node.outgoingEdges.forEach { edge ->
                sb.append("""
                edge: {source: "${edge.from.data}" target: "${edge.to.data}"}"""
                )
            }
        }
        sb.append("""
        }""")
    }



    sb.append("""
    }""")

    gdl.writeText(sb.toString())
}
