package debug.gdlClassHierarchy

import java.io.File
import org.jetbrains.kannotator.index.ClassSource
import org.jetbrains.kannotator.index.FileBasedClassSource
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.ClassData
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.declarations.*

/** dumps class hierarchy for a jar in gdl format
 * main jar gdlFile
 * */
fun main(args: Array<String>) {
    val jar = File(args[0])
    val gdl = File(args[1])

    val classSource: ClassSource = FileBasedClassSource(listOf(jar))
    val graph: HierarchyGraph<ClassData> = buildClassHierarchyGraph(classSource)

    val sb = StringBuilder()

    sb.append("""graph: {
    layoutalgorithm: minbackward
    orientation: left_to_right
    cmin: 1000
    crossing_weight: medianbary
    """)

    graph.hierarchyNodes.forEach {node ->
        sb.append("""
        node: {title:"${node.data.name}" label:"${node.data.name}"}""")

        node.outgoingEdges.forEach { edge ->
            sb.append("""
            edge: {source: "${edge.from.data.name}" target: "${edge.to.data.name}"}""")
        }
    }

    sb.append("""}""")

    gdl.writeText(sb.toString())
}