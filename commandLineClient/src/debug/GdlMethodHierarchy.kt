package debug.gdlMethodHierarchy

import java.io.File
import org.jetbrains.kannotator.index.*
import org.jetbrains.kannotator.classHierarchy.*
import org.jetbrains.kannotator.declarations.*

/** dumps class hierarchy for a jar in gdl format
 * main jar gdlFile
 * */
fun main(args: Array<String>) {
    val jar = File(args[0])
    val gdl = File(args[1])

    val classSource: ClassSource = FileBasedClassSource(listOf(jar))
    val classGraph: HierarchyGraph<ClassData> = buildClassHierarchyGraph(classSource)
    val methodGraph: HierarchyGraph<Method> = buildMethodHierarchy(classGraph)

    val sb = StringBuilder()

    sb.append("""
    graph: {
    layoutalgorithm: minbackward
    orientation: left_to_right
    cmin: 1000
    crossing_weight: medianbary
    """)

    methodGraph.hierarchyNodes.forEach {node ->
        sb.append("""
        node: {title:"${node.data}" label:"${node.data}"}""")

        node.outgoingEdges.forEach { edge ->
            sb.append("""
            edge: {source: "${edge.from.data}" target: "${edge.to.data}"}"""
            )
        }
    }

    sb.append("""
    }""")

    gdl.writeText(sb.toString())
}
