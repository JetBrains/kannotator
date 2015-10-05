package index

import java.io.File
import java.util.Comparator
import kotlinlib.*
import org.jetbrains.kannotator.index.FieldDependencyInfo
import org.jetbrains.kannotator.index.buildFieldsDependencyInfos
import org.junit.Test
import util.ClassPathDeclarationIndex
import util.ClassesFromClassPath
import util.assertEqualsOrCreate

/** checks detecting field dependencies (readers/writers) for `fieldDependency.simple.Simple` class */
class FieldDependencyTest {
    private val PATH = "testData/fieldDependency/"

    @Test fun funInDifferentClassesTest() {
        doTest("simple/simple.txt", "fieldDependency.simple.Simple")
    }

    fun doTest(expectedResultPath: String, canonicalName: String) {
        val classSource = ClassesFromClassPath(canonicalName)
        val infos = buildFieldsDependencyInfos(util.ClassPathDeclarationIndex, classSource)

        val actual = StringBuilder {
            for (fieldInfo in infos.values().sortedBy { it.field.name }.reversed()) {
                printFieldInfo(this, fieldInfo)
            }
        }.toString().trim()

        val expectedFile = File(PATH + expectedResultPath)
        assertEqualsOrCreate(expectedFile, actual)
    }

    fun printFieldInfo(sb: StringBuilder, fieldInfo: FieldDependencyInfo) {
        sb.appendln("=========== ${fieldInfo.field} ============")
        sb.appendln("== readers ==")
        if (fieldInfo.readers.isEmpty()) sb.appendln("<no readers>") else fieldInfo.readers.forEach { sb.appendln(it) }
        sb.appendln("== writers ==")
        if (fieldInfo.writers.isEmpty()) sb.appendln("<no writers>") else fieldInfo.writers.forEach { sb.appendln(it) }
    }
}
