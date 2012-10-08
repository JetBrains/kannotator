package classHierarchy

import edu.uci.ics.jung.graph.DirectedSparseMultigraph
import edu.uci.ics.jung.graph.util.EdgeType
import org.apache.commons.collections15.Transformer
import org.jetbrains.kannotator.classHierarchy.HierarchyEdge
import org.jetbrains.kannotator.classHierarchy.HierarchyGraph
import org.jetbrains.kannotator.classHierarchy.HierarchyNode
import util.getAllClassesWithPrefix
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import util.ClassesFromClassPath
import org.jetbrains.kannotator.classHierarchy.*

fun HierarchyGraph<ClassData>.toJungGraph(): DirectedSparseMultigraph<HierarchyNode<ClassData>, HierarchyEdge<ClassData>> {
    val jungGraph = DirectedSparseMultigraph<HierarchyNode<ClassData>, HierarchyEdge<ClassData>>()
    for (i in this.nodes) {
        for (e in i.children) {
            jungGraph.addEdge(e, e.parent, e.child, EdgeType.DIRECTED)
        }
    }
    return jungGraph
}

fun main(args: Array<String>) {
    val graph = buildClassHierarchyGraph(ClassesFromClassPath(getAllClassesWithPrefix("java/lang/")))

    displayJungGraph<HierarchyNode<ClassData>, HierarchyEdge<ClassData>>(
            graph.toJungGraph(),
            object : Transformer<HierarchyNode<ClassData>, String> {
                public override fun transform(classNode: HierarchyNode<ClassData>): String = classNode.name.simple
            },
            null
    )
}
