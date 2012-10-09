package classHierarchy

import java.io.File
import junit.framework.Assert
import kotlin.test.fail
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.HierarchyNode
import util.getAllClassesWithPrefix
import org.jetbrains.kannotator.classHierarchy.buildClassHierarchyGraph
import util.ClassesFromClassPath
import org.jetbrains.kannotator.classHierarchy.*

fun getClassesHierarchy(prefix: String): Collection<HierarchyNode<ClassData>> {
    val graph = buildClassHierarchyGraph(getAllClassesWithPrefix(prefix))

    return graph.nodes.filter {
        it.name.internal.startsWith(prefix)
    }.sortByToString()
}