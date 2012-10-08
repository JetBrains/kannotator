package classHierarchy

import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.classHierarchy.ClassHierarchyEdge
import org.jetbrains.kannotator.classHierarchy.ClassHierarchyGraph
import org.jetbrains.kannotator.classHierarchy.ClassNode
import util.getAllClassesWithPrefix
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import util.ClassesFromClassPath

fun ClassHierarchyGraph.toJungGraph(): DirectedSparseMultigraph<ClassNode, ClassHierarchyEdge> {
    val jungGraph = DirectedSparseMultigraph<ClassNode, ClassHierarchyEdge>()
    for (i in this.classes) {
        for (e in i.subClasses) {
            jungGraph.addEdge(e, e.base, e.derived, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

fun main(args: Array<String>) {
    val graph = buildClassHierarchyGraph(ClassesFromClassPath(getAllClassesWithPrefix("java/lang/")))

    displayJungGraph<ClassNode, ClassHierarchyEdge>(
            graph.toJungGraph(),
            object : Transformer<ClassNode, String> {
                public override fun transform(classNode: ClassNode): String = classNode.name.simple
            },
            null
    )
}
