package classHierarchy

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlinlib.sortByToString
import org.jetbrains.kannotator.classHierarchy.ClassHierarchyGraphBuilder
import org.jetbrains.kannotator.classHierarchy.ClassNode
import util.getAllClassesWithPrefix

fun getClassesHierarchy(prefix: String): Collection<ClassNode> {
    val builder = ClassHierarchyGraphBuilder()
    for (clazz in getAllClassesWithPrefix(prefix)) {
        builder.addClass(clazz)
    }

    val graph = builder.buildGraph()

    return graph.classes.filter {
        it.name.internal.startsWith(prefix)
    }.sortByToString()
}

fun assertEqualsOrCreate(expectedFile: File, actual: String) {
    if (!expectedFile.exists()) {
        expectedFile.getParentFile()!!.mkdirs()
        expectedFile.writeText(actual)
        fail("Expected data file file does not exist: ${expectedFile}. It is created from actual data")
    }

    val expected = expectedFile.readText()

    assertEquals(expected, actual)
}
